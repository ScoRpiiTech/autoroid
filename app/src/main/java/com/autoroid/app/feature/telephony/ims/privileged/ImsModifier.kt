package com.autoroid.app.feature.telephony.ims.privileged

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import rikka.shizuku.Shizuku

class ImsModifier : Instrumentation() {

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        if (arguments == null) {
            finish(Activity.RESULT_CANCELED, Bundle())
            return
        }

        // Wait for Shizuku binder ready
        var retries = 0
        while (!Shizuku.pingBinder() && retries < 50) {
            retries++
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                break
            }
        }

        val results = Bundle()
        if (retries >= 50 && !Shizuku.pingBinder()) {
            results.putBoolean(BUNDLE_RESULT, false)
            results.putString(BUNDLE_RESULT_MSG, "Shizuku binder is not ready")
            finish(Activity.RESULT_OK, results)
            return
        }

        val failure = runWithShellPermissionDelegation(TAG) {
            overrideConfig(arguments)
        }

        if (failure == null) {
            Log.i(TAG, "overrideConfig succeeded via instrumentation")
            results.putBoolean(BUNDLE_RESULT, true)
        } else {
            Log.e(TAG, "failed to override config", failure)
            results.putBoolean(BUNDLE_RESULT, false)
            results.putString(BUNDLE_RESULT_MSG, failure.toPrivilegedErrorMessage())
        }

        finish(Activity.RESULT_OK, results)
    }

    private fun overrideConfig(arguments: Bundle) {
        val cm = context.getSystemService(CarrierConfigManager::class.java)
        val sm = context.getSystemService(SubscriptionManager::class.java)

        val selectedSubId = arguments.getInt(BUNDLE_SELECT_SIM_ID, -1)
        val reset = arguments.getBoolean(BUNDLE_RESET, false)

        val subIds: IntArray = if (selectedSubId == -1) {
            try {
                val method = sm.javaClass.getMethod("getActiveSubscriptionIdList")
                method.invoke(sm) as IntArray
            } catch (_: Throwable) {
                intArrayOf()
            }
        } else {
            intArrayOf(selectedSubId)
        }

        val values = if (reset) null else toPersistableBundle(arguments)

        for (subId in subIds) {
            Log.i(TAG, "overrideConfig for subId $subId (reset=$reset)")
            try {
                val method = cm.javaClass.getMethod(
                    "overrideConfig",
                    Int::class.javaPrimitiveType,
                    PersistableBundle::class.java,
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(cm, subId, values, false)
            } catch (_: NoSuchMethodException) {
                val method = cm.javaClass.getMethod(
                    "overrideConfig",
                    Int::class.javaPrimitiveType,
                    PersistableBundle::class.java
                )
                method.invoke(cm, subId, values)
            }

            if (!reset) {
                // Apply persistent VoLTE provisioning on modem NVRAM
                applyPersistentProvisioning(subId)
            }
        }
    }

    private fun applyPersistentProvisioning(subId: Int) {
        try {
            val intType = Int::class.javaPrimitiveType!!
            val provisioningClass = Class.forName("android.telephony.ims.ProvisioningManager")
            val mmTelClass = Class.forName("android.telephony.ims.ImsMmTelManager")
            val provisioning = provisioningClass.getMethod("createForSubscriptionId", intType).invoke(null, subId)
            val mmTel = mmTelClass.getMethod("createForSubscriptionId", intType).invoke(null, subId)
            val setProvisioning = provisioningClass.getMethod("setProvisioningIntValue", intType, intType)
            val setUser = mmTelClass.getMethod("setAdvancedCallingSettingEnabled", Boolean::class.javaPrimitiveType)
            val optInKey = provisioningClass.getField("KEY_VOIMS_OPT_IN_STATUS").getInt(null)

            setProvisioning.invoke(provisioning, optInKey, 1)
            setUser.invoke(mmTel, true)

            val setProperty = SubscriptionManager::class.java.getMethod(
                "setSubscriptionProperty", intType, String::class.java, String::class.java
            )
            setProperty.invoke(null, subId, "ENHANCED_4G_MODE_ENABLED", "1")
            setProperty.invoke(null, subId, "VOIMS_OPT_IN_STATUS", "1")
            Log.i(TAG, "VoLTE persistent provisioning successfully set for subId $subId")
        } catch (t: Throwable) {
            Log.w(TAG, "VoLTE persistent provisioning note: ${t.message}")
        }
    }

    private fun toPersistableBundle(bundle: Bundle): PersistableBundle {
        val pb = PersistableBundle()
        for (key in bundle.keySet()) {
            if (key == BUNDLE_SELECT_SIM_ID || key == BUNDLE_RESET) continue
            val value = bundle.get(key)
            when (value) {
                is Boolean -> pb.putBoolean(key, value)
                is Int -> pb.putInt(key, value)
                is Long -> pb.putLong(key, value)
                is Double -> pb.putDouble(key, value)
                is String -> pb.putString(key, value)
                is IntArray -> pb.putIntArray(key, value)
                is LongArray -> pb.putLongArray(key, value)
                is DoubleArray -> pb.putDoubleArray(key, value)
                is BooleanArray -> pb.putBooleanArray(key, value)
                is Array<*> -> {
                    if (value.isArrayOf<String>()) {
                        @Suppress("UNCHECKED_CAST")
                        pb.putStringArray(key, value as Array<String>)
                    }
                }
            }
        }
        return pb
    }

    companion object {
        const val TAG = "ImsModifier"
        const val BUNDLE_SELECT_SIM_ID = "select_sim_id"
        const val BUNDLE_RESET = "reset"
        const val BUNDLE_RESULT = "result"
        const val BUNDLE_RESULT_MSG = "result_msg"
    }
}
