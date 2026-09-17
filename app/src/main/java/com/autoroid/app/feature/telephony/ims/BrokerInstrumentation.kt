package com.autoroid.app.feature.telephony.ims

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.system.Os
import android.telephony.CarrierConfigManager
import android.util.Log
import com.autoroid.app.feature.telephony.TelephonyController
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Instrumentation runner executed under the shell identity (via `am instrument`).
 *
 * This provides the definitive bypass for CVE-2025-48617 on Google Pixel devices
 * running modern Android (Android 14, 15, 16). By executing within an active instrumentation,
 * it delegates shell permission identity (`MODIFY_PHONE_STATE`) to safely call
 * `CarrierConfigManager.overrideConfig(subId, bundle, false)` without throwing
 * `SecurityException: overrideConfig cannot be invoked by shell`.
 */
class BrokerInstrumentation : Instrumentation() {

    companion object {
        private const val TAG = "BrokerInstrumentation"
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        val returnBundle = Bundle()

        if (arguments == null) {
            returnBundle.putString("result", "error: arguments null")
            finish(android.app.Activity.RESULT_CANCELED, returnBundle)
            return
        }

        val clear = arguments.getBoolean("moder_clear", false) ||
            arguments.getString("moder_clear").equals("true", ignoreCase = true)
        val subId = arguments.getInt("moder_subId", -1).let { id ->
            if (id != -1) id else arguments.getString("moder_subId")?.toIntOrNull() ?: -1
        }

        try {
            if (subId < 0) {
                returnBundle.putString("result", "error: invalid subId $subId")
                finish(android.app.Activity.RESULT_CANCELED, returnBundle)
                return
            }

            if (clear) {
                clearConfig(subId)
            } else {
                applyConfig(subId, arguments)
            }
            returnBundle.putString("result", "success")
            finish(android.app.Activity.RESULT_OK, returnBundle)
        } catch (e: Throwable) {
            Log.e(TAG, "Error in BrokerInstrumentation execution: ${e.message}", e)
            returnBundle.putString("result", "error: ${e.message}")
            finish(android.app.Activity.RESULT_CANCELED, returnBundle)
        }
    }

    @SuppressLint("MissingPermission")
    private fun applyConfig(subId: Int, arguments: Bundle) {
        Log.i(TAG, "applyConfig started for subId: $subId")
        TelephonyController.ensureHiddenApiExempted()

        var amInstance: Any? = null
        try {
            val binder = SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
            if (binder != null) {
                val wrapped = ShizukuBinderWrapper(binder)
                val stubClass = Class.forName("android.app.IActivityManager\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                amInstance = asInterface.invoke(null, wrapped)
                val startDelegate = amInstance.javaClass.getMethod(
                    "startDelegateShellPermissionIdentity",
                    Int::class.javaPrimitiveType,
                    Array<String>::class.java
                )
                startDelegate.invoke(amInstance, Os.getuid(), null)
                Log.i(TAG, "Delegated shell permission identity acquired.")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Shell permission identity delegation note: ${e.message}")
        }

        try {
            val configManager = context.getSystemService(CarrierConfigManager::class.java)
            val overrideValues = PersistableBundle()

            for (key in arguments.keySet()) {
                if (key.startsWith("moder_")) continue
                when (val value = arguments.get(key)) {
                    is Boolean -> overrideValues.putBoolean(key, value)
                    is Int -> overrideValues.putInt(key, value)
                    is Long -> overrideValues.putLong(key, value)
                    is Double -> overrideValues.putDouble(key, value)
                    is String -> {
                        when {
                            value.equals("true", ignoreCase = true) -> overrideValues.putBoolean(key, true)
                            value.equals("false", ignoreCase = true) -> overrideValues.putBoolean(key, false)
                            value.toIntOrNull() != null -> overrideValues.putInt(key, value.toInt())
                            else -> overrideValues.putString(key, value)
                        }
                    }
                    is BooleanArray -> overrideValues.putBooleanArray(key, value)
                    is IntArray -> overrideValues.putIntArray(key, value)
                    is LongArray -> overrideValues.putLongArray(key, value)
                    is Array<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        if (value.isArrayOf<String>()) {
                            overrideValues.putStringArray(key, value as Array<String>)
                        }
                    }
                }
            }

            try {
                // Must be persistent = false on Android 14/15/16 (prevents crashes from system app check)
                val method = configManager.javaClass.getMethod(
                    "overrideConfig",
                    Int::class.javaPrimitiveType,
                    PersistableBundle::class.java,
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(configManager, subId, overrideValues, false)
                Log.i(TAG, "Successfully overrode carrier config for subId $subId")
            } catch (e: Throwable) {
                Log.e(TAG, "CarrierConfigManager.overrideConfig error: ${e.message}", e)
                throw e
            }
        } finally {
            if (amInstance != null) {
                try {
                    val stopDelegate = amInstance.javaClass.getMethod("stopDelegateShellPermissionIdentity")
                    stopDelegate.invoke(amInstance)
                    Log.i(TAG, "Delegated shell permission identity released.")
                } catch (_: Throwable) {}
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun clearConfig(subId: Int) {
        Log.i(TAG, "clearConfig started for subId: $subId")
        TelephonyController.ensureHiddenApiExempted()

        var amInstance: Any? = null
        try {
            val binder = SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
            if (binder != null) {
                val wrapped = ShizukuBinderWrapper(binder)
                val stubClass = Class.forName("android.app.IActivityManager\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                amInstance = asInterface.invoke(null, wrapped)
                val startDelegate = amInstance.javaClass.getMethod(
                    "startDelegateShellPermissionIdentity",
                    Int::class.javaPrimitiveType,
                    Array<String>::class.java
                )
                startDelegate.invoke(amInstance, Os.getuid(), null)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Shell permission identity delegation note: ${e.message}")
        }

        try {
            val configManager = context.getSystemService(CarrierConfigManager::class.java)
            val method = configManager.javaClass.getMethod(
                "overrideConfig",
                Int::class.javaPrimitiveType,
                PersistableBundle::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(configManager, subId, null, false)
            Log.i(TAG, "Successfully cleared carrier config overrides for subId $subId")
        } catch (e: Throwable) {
            Log.e(TAG, "clearConfig error: ${e.message}", e)
            throw e
        } finally {
            if (amInstance != null) {
                try {
                    val stopDelegate = amInstance.javaClass.getMethod("stopDelegateShellPermissionIdentity")
                    stopDelegate.invoke(amInstance)
                } catch (_: Throwable) {}
            }
        }
    }
}
