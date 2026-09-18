package com.autoroid.app.feature.telephony.ims

import android.app.Activity
import android.app.IActivityManager
import android.app.IInstrumentationWatcher
import android.app.UiAutomationConnection
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.os.ServiceManager
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.telephony.TelephonyController
import com.autoroid.app.feature.telephony.ims.model.ImsConfig
import com.autoroid.app.feature.telephony.ims.privileged.BrokerInstrumentation
import com.autoroid.app.feature.telephony.ims.privileged.ImsCapabilityReader
import com.autoroid.app.feature.telephony.ims.privileged.ImsModifier
import com.autoroid.app.feature.telephony.ims.privileged.ImsResetter
import com.autoroid.app.feature.telephony.ims.privileged.isCarrierConfigPermissionError
import com.autoroid.app.feature.telephony.ims.repository.ImsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

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
     * Uses privileged Instrumentation with INSTR_FLAG_NO_RESTART (flag 8) via Shizuku
     * to completely bypass Google Pixel's CVE-2025-48617 restriction without restarting the app.
     */
    suspend fun applyConfig(slotIndex: Int, subId: Int, config: ImsConfig): Result<String> = withContext(Dispatchers.IO) {
        _isApplying.value = true
        val updatedConfig = config.copy(slotIndex = slotIndex, subscriptionId = subId)

        try {
            var isSuccess = false
            var errorMessage: String? = null

            // Method 1: Privileged Instrumentation via Shizuku (Exact Turbo IMS / TensorIMS bypass)
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    val (success, errorMsg) = overrideImsConfig(subId, updatedConfig)
                    if (success) {
                        isSuccess = true
                    } else {
                        errorMessage = errorMsg
                    }
                } else {
                    errorMessage = "Shizuku permission not granted. Please authorize Autoroid in Shizuku."
                }
            } else {
                errorMessage = "Shizuku service is not running. Please start Shizuku."
            }

            // Method 2: Root fallback (UID 0)
            if (!isSuccess && privilegeManager.currentLevel.value == PrivilegeLevel.ROOT) {
                val rootRes = applyViaRoot(subId, updatedConfig.toCarrierConfigBundle())
                if (rootRes) {
                    isSuccess = true
                    errorMessage = null
                }
            }

            if (isSuccess) {
                repository.saveConfig(updatedConfig.copy(isApplied = true, lastAppliedTimestamp = System.currentTimeMillis()))
                val msg = "IMS overrides successfully applied for SIM ${slotIndex + 1} (SubId $subId)"
                _lastResult.value = msg
                Log.i(TAG, msg)
                Result.success(msg)
            } else {
                val err = errorMessage ?: "Failed to apply IMS overrides. Ensure Shizuku or Root is granted."
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
     * Executes two-tier IMS configuration override matching TensorIMS:
     * 1. Primary path: ImsModifier (carrier override + persistent modem NVRAM provisioning)
     * 2. Fallback path: BrokerInstrumentation (pure carrier override under shell delegation)
     */
    private suspend fun overrideImsConfig(
        subId: Int,
        config: ImsConfig
    ): Pair<Boolean, String?> {
        val primaryArgs = config.toBundle().apply {
            putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, subId)
            putBoolean(ImsModifier.BUNDLE_RESET, false)
        }

        Log.i(TAG, "overrideImsConfig: attempting via ImsModifier for subId $subId")
        val result = startInstrumentation(context, ImsModifier::class.java, primaryArgs, receiveResult = true)
        if (result == null) {
            Log.w(TAG, "ImsModifier returned empty result, falling back to BrokerInstrumentation")
            return tryOverrideWithBroker(subId, config, "ImsModifier returned empty result")
        }

        if (result.getBoolean(ImsModifier.BUNDLE_RESULT, false)) {
            Log.i(TAG, "ImsModifier succeeded for subId $subId")
            return Pair(true, null)
        }

        val msg = result.getString(ImsModifier.BUNDLE_RESULT_MSG) ?: "Unknown ImsModifier error"
        Log.w(TAG, "ImsModifier failed: $msg. Trying fallback to BrokerInstrumentation...")
        return tryOverrideWithBroker(subId, config, msg)
    }

    private suspend fun tryOverrideWithBroker(
        subId: Int,
        config: ImsConfig,
        primaryError: String
    ): Pair<Boolean, String?> {
        if (!shouldRetryWithBroker(primaryError)) {
            return Pair(false, primaryError)
        }

        val brokerArgs = config.toBundle().apply {
            putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, subId)
            putBoolean(ImsModifier.BUNDLE_RESET, false)
        }

        val brokerResult = startInstrumentation(context, BrokerInstrumentation::class.java, brokerArgs, receiveResult = true)
        if (brokerResult == null) {
            return Pair(false, "$primaryError\nBroker fallback: failed with empty result")
        }

        if (brokerResult.getBoolean(ImsModifier.BUNDLE_RESULT, false)) {
            Log.i(TAG, "BrokerInstrumentation fallback succeeded for subId $subId")
            return Pair(true, null)
        }

        val brokerMsg = brokerResult.getString(ImsModifier.BUNDLE_RESULT_MSG) ?: "Unknown Broker error"
        return Pair(false, "$primaryError\nBroker fallback: $brokerMsg")
    }

    private fun shouldRetryWithBroker(msg: String): Boolean {
        return isCarrierConfigPermissionError(msg) ||
                msg.contains("empty result", ignoreCase = true) ||
                msg.contains("SecurityException", ignoreCase = true) ||
                msg.contains("NoSuchFieldException", ignoreCase = true) ||
                msg.contains("LinkageError", ignoreCase = true)
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
            var isSuccess = false

            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                val args = Bundle().apply {
                    putInt(ImsResetter.BUNDLE_SELECT_SIM_ID, subId)
                }
                val result = startInstrumentation(context, ImsResetter::class.java, args, receiveResult = true)
                if (result != null) {
                    isSuccess = result.getBoolean(ImsResetter.BUNDLE_RESULT, false)
                }

                if (!isSuccess) {
                    val brokerArgs = Bundle().apply {
                        putInt(ImsModifier.BUNDLE_SELECT_SIM_ID, subId)
                        putBoolean(ImsModifier.BUNDLE_RESET, true)
                    }
                    val brokerResult = startInstrumentation(context, BrokerInstrumentation::class.java, brokerArgs, receiveResult = true)
                    if (brokerResult != null) {
                        isSuccess = brokerResult.getBoolean(ImsModifier.BUNDLE_RESULT, false)
                    }
                }
            }

            if (!isSuccess && privilegeManager.currentLevel.value == PrivilegeLevel.ROOT) {
                isSuccess = clearViaRoot(subId)
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
     * Queries live IMS registration, VoLTE, VoWiFi, and VoNR status for a subscription.
     */
    suspend fun queryCapabilities(subId: Int): Bundle? = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return@withContext null
        }
        val args = Bundle().apply {
            putInt(ImsCapabilityReader.BUNDLE_SELECT_SIM_ID, subId)
        }
        startInstrumentation(context, ImsCapabilityReader::class.java, args, receiveResult = true)
    }

    /**
     * Invoked during device boot (`BOOT_COMPLETED`) or application startup.
     * Restores all persisted IMS configurations automatically across reboots.
     */
    suspend fun onBoot() = withContext(Dispatchers.IO) {
        try {
            delay(2500)
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
     * Launches a privileged Instrumentation component via Shizuku's IActivityManager IPC.
     * Uses INSTR_FLAG_NO_RESTART (flag 8) to preserve the running app process.
     */
    private suspend fun startInstrumentation(
        context: Context,
        cls: Class<*>,
        args: Bundle?,
        receiveResult: Boolean
    ): Bundle? = instrumentationMutex.withLock {
        val previous = activeInstrumentation
        if (previous != null && !previous.isCompleted) {
            withTimeoutOrNull(INSTRUMENTATION_TIMEOUT_MS) {
                previous.await()
            }
        }

        val deferredResult = CompletableDeferred<Bundle?>()
        var watcher: IInstrumentationWatcher.Stub? = null

        if (receiveResult) {
            watcher = object : IInstrumentationWatcher.Stub() {
                override fun instrumentationStatus(
                    name: ComponentName?,
                    resultCode: Int,
                    results: Bundle?
                ) {}

                override fun instrumentationFinished(
                    name: ComponentName?,
                    resultCode: Int,
                    results: Bundle?
                ) {
                    if (resultCode != Activity.RESULT_OK) {
                        Log.w(TAG, "Instrumentation finished with resultCode=$resultCode for $name")
                        deferredResult.complete(null)
                    } else {
                        deferredResult.complete(results)
                    }
                }
            }
        }

        try {
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Shizuku binder is unavailable")
                return@withLock null
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Shizuku permission not granted")
                return@withLock null
            }

            TelephonyController.ensureHiddenApiExempted()
            val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
                ?: error("Activity service unavailable")
            val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))

            val name = ComponentName(context, cls)
            // INSTR_FLAG_NO_RESTART (1 << 3 = 8) keeps the running process alive!
            val flags = 8
            val connection = UiAutomationConnection()

            Log.d(TAG, "Calling am.startInstrumentation for $name with flags=$flags")
            val started = am.startInstrumentation(
                name,
                null,
                flags,
                args,
                watcher,
                connection,
                0,
                null
            )

            if (!started) {
                Log.e(TAG, "am.startInstrumentation rejected for $name")
                return@withLock null
            }

            if (receiveResult) {
                activeInstrumentation = deferredResult
                return@withLock withTimeoutOrNull(INSTRUMENTATION_TIMEOUT_MS) {
                    deferredResult.await()
                }
            }
            return@withLock null
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start instrumentation", t)
            return@withLock null
        }
    }

    private suspend fun applyViaRoot(subId: Int, bundle: PersistableBundle): Boolean {
        return try {
            privilegeManager.executeElevated("pm grant com.autoroid.app android.permission.MODIFY_PHONE_STATE")
            val configManager = context.getSystemService(CarrierConfigManager::class.java)
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
            val configManager = context.getSystemService(CarrierConfigManager::class.java)
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
        private const val INSTRUMENTATION_TIMEOUT_MS = 15_000L
        private val instrumentationMutex = Mutex()
        private var activeInstrumentation: CompletableDeferred<Bundle?>? = null
    }
}
