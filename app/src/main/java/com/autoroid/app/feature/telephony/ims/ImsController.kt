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
     * Uses Shizuku direct Binder IPC (ICarrierConfigLoader) first,
     * falling back to elevated shell (`cmd phone cc set-value`).
     */
    suspend fun applyConfig(slotIndex: Int, subId: Int, config: ImsConfig): Result<String> = withContext(Dispatchers.IO) {
        _isApplying.value = true
        val updatedConfig = config.copy(slotIndex = slotIndex, subscriptionId = subId)

        try {
            // Method 1: Elevated Instrumentation runner (Bypasses CVE-2025-48617 on Android 14/15/16)
            val (instSuccess, instError) = applyViaInstrumentation(subId, updatedConfig)

            // Method 2: Direct Shizuku Binder IPC fallback (for older Android versions)
            val binderSuccess = if (!instSuccess) {
                applyViaShizukuBinder(subId, updatedConfig)
            } else {
                true
            }

            if (instSuccess || binderSuccess) {
                repository.saveConfig(updatedConfig.copy(isApplied = true, lastAppliedTimestamp = System.currentTimeMillis()))
                val msg = "IMS overrides successfully applied for SIM ${slotIndex + 1} (SubId $subId)"
                _lastResult.value = msg
                Result.success(msg)
            } else {
                val detail = if (!instError.isNullOrBlank()) ": $instError" else ""
                val err = "Failed to apply IMS overrides$detail. Ensure Shizuku or Root is granted."
                _lastResult.value = err
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            val err = "Error applying IMS overrides: ${e.message}"
            _lastResult.value = err
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
        } finally {
            _isApplying.value = false
        }
    }

    /**
     * Reverts carrier configurations to factory defaults for a specific SIM slot.
     */
    suspend fun clearConfig(slotIndex: Int, subId: Int): Result<String> = withContext(Dispatchers.IO) {
        _isApplying.value = true
        try {
            // 1. Try BrokerInstrumentation clear (Bypasses CVE-2025-48617)
            val (instSuccess, _) = clearViaInstrumentation(subId)

            // 2. Try Shizuku binder empty override fallback
            if (!instSuccess) {
                try {
                    clearViaShizukuBinder(subId)
                } catch (_: Throwable) {}
            }

            repository.markApplied(slotIndex, false)
            val msg = "Carrier overrides cleared for SIM ${slotIndex + 1}. Factory configs restored."
            _lastResult.value = msg
            Result.success(msg)
        } catch (e: Exception) {
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
            // If the user previously applied the config, re-apply it automatically
            if (config.isApplied || config.volteEnabled) {
                val targetSubId = if (sim.subscriptionId > 0) sim.subscriptionId else sim.slotIndex + 1
                applyConfig(sim.slotIndex, targetSubId, config)
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
            // CRITICAL: persistent = false prevents crashes on Android 14/15/16
            method.invoke(loader, subId, bundle, false)
            true
        } catch (_: Throwable) {
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
            method.invoke(loader, subId, PersistableBundle.EMPTY, false)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun applyViaInstrumentation(subId: Int, config: ImsConfig): Pair<Boolean, String?> {
        val sb = StringBuilder()
        sb.append("am instrument -w ")
        sb.append("-e moder_subId ").append(subId).append(" ")
        for ((key, value) in config.toKeyValuePairs()) {
            sb.append("-e ").append(key).append(" ").append(value).append(" ")
        }
        sb.append("com.autoroid.app/.feature.telephony.ims.BrokerInstrumentation")

        val result = privilegeManager.executeElevated(sb.toString())
        val output = "${result.stdout}\n${result.stderr}".trim()
        Log.i("ImsController", "am instrument apply output: $output")

        val isSuccess = output.contains("result=success") ||
            (result.isSuccess && !output.contains("result=error") && output.contains("INSTRUMENTATION_CODE: -1"))

        val errorDetails = if (!isSuccess) {
            val errorMatch = Regex("result=error:\\s*(.*)").find(output)
            errorMatch?.groupValues?.get(1)?.trim() ?: output.ifBlank { result.stderr }
        } else null

        return Pair(isSuccess, errorDetails)
    }

    private suspend fun clearViaInstrumentation(subId: Int): Pair<Boolean, String?> {
        val cmd = "am instrument -w -e moder_clear true -e moder_subId $subId com.autoroid.app/.feature.telephony.ims.BrokerInstrumentation"
        val result = privilegeManager.executeElevated(cmd)
        val output = "${result.stdout}\n${result.stderr}".trim()
        Log.i("ImsController", "am instrument clear output: $output")

        val isSuccess = output.contains("result=success") ||
            (result.isSuccess && !output.contains("result=error") && output.contains("INSTRUMENTATION_CODE: -1"))

        return Pair(isSuccess, if (!isSuccess) output else null)
    }
}
