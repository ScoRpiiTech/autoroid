package com.autoroid.app.feature.telephony.ims

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.telephony.TelephonyController
import com.autoroid.app.feature.telephony.ims.model.ImsConfig
import com.autoroid.app.feature.telephony.ims.repository.ImsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ImsController(
    private val context: Context,
    private val repository: ImsRepository,
    private val telephonyController: TelephonyController,
    private val privilegeManager: PrivilegeManager
) {

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    private val _lastResult = MutableStateFlow<String?>(null)
    val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

    /**
     * Applies carrier config & IMS overrides for a specific SIM slot.
     * Uses in-process shell permission delegation via Shizuku first (bypasses CVE-2025-48617),
     * falling back to direct Shizuku Binder IPC and Root elevated execution.
     */
    suspend fun applyConfig(slotIndex: Int, subId: Int, config: ImsConfig): Result<String> = withContext(Dispatchers.IO) {
        _isApplying.value = true
        val updatedConfig = config.copy(slotIndex = slotIndex, subscriptionId = subId)
        val bundle = updatedConfig.toCarrierConfigBundle()

        try {
            // Method 1: In-process Shell Permission Delegation via Shizuku (Bypasses CVE-2025-48617 on Pixel)
            var success = applyWithShellDelegation(subId, bundle)

            // Method 2: Direct Shizuku Binder IPC fallback (ICarrierConfigLoader)
            if (!success) {
                success = applyViaShizukuBinder(subId, updatedConfig)
            }

            // Method 3: Root permission grant & override
            if (!success && privilegeManager.currentLevel.value == PrivilegeLevel.ROOT) {
                success = applyViaRoot(subId, bundle)
            }

            if (success) {
                repository.saveConfig(updatedConfig.copy(isApplied = true, lastAppliedTimestamp = System.currentTimeMillis()))
                val msg = "IMS overrides successfully applied for SIM ${slotIndex + 1} (SubId $subId)"
                _lastResult.value = msg
                Log.i(TAG, msg)
                Result.success(msg)
            } else {
                val err = "Failed to apply IMS overrides. Ensure Shizuku or Root is granted."
                _lastResult.value = err
                Log.w(TAG, err)
                Result.failure(Exception(err))
            }
        } catch (e: Throwable) {
            val err = "Error applying IMS overrides: ${e.message}"
            _lastResult.value = err
            Log.e(TAG, err, e)
            Result.failure(e)
        } finally {
            _isApplying.value = false
        }
    }

    /**
     * Applies saved IMS overrides to all currently active SIM cards (Physical + eSIM).
     */
    suspend fun applyToAllActiveSims(): Result<String> = withContext(Dispatchers.IO) {
        _isApplying.value = true
        try {
            telephonyController.refreshSimState()
            val activeSims = telephonyController.simSlots.value

            if (activeSims.isEmpty()) {
                val err = "No active SIM cards detected to apply IMS overrides."
                _lastResult.value = err
                return@withContext Result.failure(Exception(err))
            }

            val appliedNames = mutableListOf<String>()
            for (sim in activeSims) {
                val savedConfig = repository.getConfig(sim.slotIndex, sim.subscriptionId)
                val targetSubId = if (sim.subscriptionId > 0) sim.subscriptionId else sim.slotIndex + 1
                val res = applyConfig(sim.slotIndex, targetSubId, savedConfig)
                if (res.isSuccess) {
                    appliedNames.add(sim.displayLabel)
                }
            }

            if (appliedNames.isNotEmpty()) {
                val msg = "IMS overrides active on: ${appliedNames.joinToString(", ")}"
                _lastResult.value = msg
                Result.success(msg)
            } else {
                val err = "Failed to apply overrides to any active SIM."
                _lastResult.value = err
                Result.failure(Exception(err))
            }
        } catch (e: Throwable) {
            val err = "Error applying to all SIMs: ${e.message}"
            _lastResult.value = err
            Result.failure(e)
        } finally {
            _isApplying.value = false
        }
    }

    /**
     * Clears all carrier overrides for a SIM slot, returning it to factory defaults.
     */
    suspend fun clearConfig(slotIndex: Int, subId: Int): Result<String> = withContext(Dispatchers.IO) {
        _isApplying.value = true
        try {
            // 1. Try shell delegation clear
            var success = clearWithShellDelegation(subId)

            // 2. Try Shizuku binder empty override fallback
            if (!success) {
                success = clearViaShizukuBinder(subId)
            }

            // 3. Try Root clear
            if (!success && privilegeManager.currentLevel.value == PrivilegeLevel.ROOT) {
                success = clearViaRoot(subId)
            }

            repository.markApplied(slotIndex, false)
            val msg = "Carrier overrides cleared for SIM ${slotIndex + 1}. Factory configs restored."
            _lastResult.value = msg
            Result.success(msg)
        } catch (e: Throwable) {
            val err = "Failed to clear carrier configs: ${e.message}"
            _lastResult.value = err
            Result.failure(e)
        } finally {
            _isApplying.value = false
        }
    }

    /**
     * Invoked during device boot (`BOOT_COMPLETED`) or application startup.
     * Restores all persisted IMS configurations automatically across reboots.
     */
    suspend fun onBoot() = withContext(Dispatchers.IO) {
        try {
            delay(2500) // Brief pause to allow telephony and privilege daemons to stabilize
            privilegeManager.refresh()
            val currentPrivilege = privilegeManager.currentLevel.value
            if (currentPrivilege == PrivilegeLevel.NONE) {
                return@withContext
            }

            telephonyController.refreshSimState()
            val activeSims = telephonyController.simSlots.value

            for (sim in activeSims) {
                val config = repository.getConfig(sim.slotIndex, sim.subscriptionId)
                if (config.isApplied || config.volteEnabled) {
                    val targetSubId = if (sim.subscriptionId > 0) sim.subscriptionId else sim.slotIndex + 1
                    applyConfig(sim.slotIndex, targetSubId, config)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "onBoot auto-apply note: ${t.message}")
        }
    }

    /**
     * In-process shell permission delegation via Shizuku IPC.
     * Adopts shell permissions on our UID, calls CarrierConfigManager.overrideConfig(), and releases delegation.
     */
    private fun applyWithShellDelegation(subId: Int, bundle: PersistableBundle): Boolean {
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        var amInstance: Any? = null
        return try {
            TelephonyController.ensureHiddenApiExempted()
            val amBinder = SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE) ?: return false
            val wrappedAm = ShizukuBinderWrapper(amBinder)
            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            amInstance = asInterface.invoke(null, wrappedAm)

            val startDelegate = amInstance.javaClass.getMethod(
                "startDelegateShellPermissionIdentity",
                Int::class.javaPrimitiveType,
                Array<String>::class.java
            )
            startDelegate.invoke(amInstance, android.os.Process.myUid(), null)
            Log.i(TAG, "Shell permission identity delegated to UID ${android.os.Process.myUid()}")

            val configManager = context.getSystemService(android.telephony.CarrierConfigManager::class.java)
            val overrideMethod = configManager.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            )
            overrideMethod.invoke(configManager, subId, bundle, false)
            Log.i(TAG, "CarrierConfigManager.overrideConfig succeeded with shell delegation for subId $subId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "applyWithShellDelegation note: ${e.message}")
            false
        } finally {
            if (amInstance != null) {
                try {
                    val stopDelegate = amInstance.javaClass.getMethod("stopDelegateShellPermissionIdentity")
                    stopDelegate.invoke(amInstance)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun clearWithShellDelegation(subId: Int): Boolean {
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        var amInstance: Any? = null
        return try {
            TelephonyController.ensureHiddenApiExempted()
            val amBinder = SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE) ?: return false
            val wrappedAm = ShizukuBinderWrapper(amBinder)
            val stubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            amInstance = asInterface.invoke(null, wrappedAm)

            val startDelegate = amInstance.javaClass.getMethod(
                "startDelegateShellPermissionIdentity",
                Int::class.javaPrimitiveType,
                Array<String>::class.java
            )
            startDelegate.invoke(amInstance, android.os.Process.myUid(), null)

            val configManager = context.getSystemService(android.telephony.CarrierConfigManager::class.java)
            val overrideMethod = configManager.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            )
            overrideMethod.invoke(configManager, subId, null, false)
            Log.i(TAG, "CarrierConfigManager.overrideConfig cleared for subId $subId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "clearWithShellDelegation note: ${e.message}")
            false
        } finally {
            if (amInstance != null) {
                try {
                    val stopDelegate = amInstance.javaClass.getMethod("stopDelegateShellPermissionIdentity")
                    stopDelegate.invoke(amInstance)
                } catch (_: Throwable) {}
            }
        }
    }

    private fun applyViaShizukuBinder(subId: Int, config: ImsConfig): Boolean {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return false
            }

            TelephonyController.ensureHiddenApiExempted()
            val binder = SystemServiceHelper.getSystemService("carrier_config") ?: return false
            val wrapped = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
            val loader = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, wrapped) ?: return false

            val method = loader.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            )

            val bundle = config.toCarrierConfigBundle()
            method.invoke(loader, subId, bundle, false)
            Log.i(TAG, "applyViaShizukuBinder succeeded for subId $subId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "applyViaShizukuBinder note: ${e.message}")
            false
        }
    }

    private fun clearViaShizukuBinder(subId: Int): Boolean {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            TelephonyController.ensureHiddenApiExempted()
            val binder = SystemServiceHelper.getSystemService("carrier_config") ?: return false
            val wrapped = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
            val loader = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, wrapped) ?: return false

            val method = loader.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(loader, subId, null, false)
            Log.i(TAG, "clearViaShizukuBinder succeeded for subId $subId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "clearViaShizukuBinder note: ${e.message}")
            false
        }
    }

    private suspend fun applyViaRoot(subId: Int, bundle: PersistableBundle): Boolean {
        return try {
            privilegeManager.executeElevated("pm grant com.autoroid.app android.permission.MODIFY_PHONE_STATE")
            val configManager = context.getSystemService(android.telephony.CarrierConfigManager::class.java)
            val overrideMethod = configManager.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            )
            overrideMethod.invoke(configManager, subId, bundle, false)
            Log.i(TAG, "applyViaRoot succeeded for subId $subId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "applyViaRoot note: ${e.message}")
            false
        }
    }

    private suspend fun clearViaRoot(subId: Int): Boolean {
        return try {
            privilegeManager.executeElevated("pm grant com.autoroid.app android.permission.MODIFY_PHONE_STATE")
            val configManager = context.getSystemService(android.telephony.CarrierConfigManager::class.java)
            val overrideMethod = configManager.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            )
            overrideMethod.invoke(configManager, subId, null, false)
            Log.i(TAG, "clearViaRoot succeeded for subId $subId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "clearViaRoot note: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "ImsController"
    }
}
