package com.autoroid.app.feature.telephony.ims.privileged

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.android.internal.telephony.ITelephony
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ImsResetter : Instrumentation() {

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        if (arguments == null) {
            finish(Activity.RESULT_CANCELED, Bundle())
            return
        }

        var retries = 0
        while (!Shizuku.pingBinder() && retries < 50) {
            retries++
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                break
            }
        }

        val result = Bundle()
        val failure = runWithShellPermissionDelegation(TAG) {
            val subId = arguments.getInt(BUNDLE_SELECT_SIM_ID, -1)
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val cm = context.getSystemService(CarrierConfigManager::class.java)

            val subIds: IntArray = if (subId == -1) {
                try {
                    val method = sm.javaClass.getMethod("getActiveSubscriptionIdList")
                    method.invoke(sm) as IntArray
                } catch (_: Throwable) {
                    intArrayOf()
                }
            } else {
                intArrayOf(subId)
            }

            // Clear carrier configs
            for (id in subIds) {
                try {
                    val method = cm.javaClass.getMethod(
                        "overrideConfig",
                        Int::class.javaPrimitiveType,
                        android.os.PersistableBundle::class.java,
                        Boolean::class.javaPrimitiveType
                    )
                    method.invoke(cm, id, null, false)
                } catch (_: NoSuchMethodException) {
                    val method = cm.javaClass.getMethod(
                        "overrideConfig",
                        Int::class.javaPrimitiveType,
                        android.os.PersistableBundle::class.java
                    )
                    method.invoke(cm, id, null)
                }
            }

            // Attempt telephony resetIms if available
            try {
                val binder = SystemServiceHelper.getSystemService("phone")
                if (binder != null) {
                    val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                    val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                    val telephony = asInterface.invoke(null, ShizukuBinderWrapper(binder)) as ITelephony
                    for (id in subIds) {
                        try {
                            val subInfo = sm.getActiveSubscriptionInfo(id)
                            val slot = subInfo?.simSlotIndex ?: 0
                            telephony.resetIms(slot)
                            Log.i(TAG, "resetIms called for slot $slot")
                        } catch (t: Throwable) {
                            Log.w(TAG, "resetIms slot call note: ${t.message}")
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "telephony resetIms note: ${t.message}")
            }
        }

        if (failure == null) {
            result.putBoolean(BUNDLE_RESULT, true)
        } else {
            Log.e(TAG, "reset ims failed", failure)
            result.putBoolean(BUNDLE_RESULT, false)
            result.putString(BUNDLE_RESULT_MSG, failure.toPrivilegedErrorMessage())
        }
        finish(Activity.RESULT_OK, result)
    }

    companion object {
        const val TAG = "ImsResetter"
        const val BUNDLE_SELECT_SIM_ID = "select_sim_id"
        const val BUNDLE_RESULT = "result"
        const val BUNDLE_RESULT_MSG = "result_msg"
    }
}
