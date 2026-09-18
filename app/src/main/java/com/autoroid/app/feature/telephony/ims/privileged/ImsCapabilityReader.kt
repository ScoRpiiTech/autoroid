package com.autoroid.app.feature.telephony.ims.privileged

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.os.ServiceManager
import android.telephony.AccessNetworkConstants
import android.telephony.NetworkRegistrationInfo
import android.telephony.TelephonyManager
import android.util.Log
import com.android.internal.telephony.ITelephony
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

class ImsCapabilityReader : Instrumentation() {

    @SuppressLint("MissingPermission")
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        if (arguments == null) {
            finish(Activity.RESULT_CANCELED, Bundle())
            return
        }

        val result = Bundle()
        val subId = arguments.getInt(BUNDLE_SELECT_SIM_ID, -1)
        if (subId < 0) {
            result.putString(BUNDLE_RESULT_MSG, "Invalid subId")
            finish(Activity.RESULT_OK, result)
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

        val failure = runWithShellPermissionDelegation(TAG) {
            // 1. IMS Registered check via telephony stub
            try {
                val binder = ServiceManager.getService("phone")
                if (binder != null) {
                    val telephony = ITelephony.Stub.asInterface(ShizukuBinderWrapper(binder))
                    result.putBoolean(BUNDLE_IMS_REGISTERED, telephony.isImsRegistered(subId))
                }
            } catch (t: Throwable) {
                Log.w(TAG, "ITelephony isImsRegistered note: ${t.message}")
            }

            // 2. Query MmTel capabilities via reflection on ImsMmTelManager
            try {
                val mmTelClass = Class.forName("android.telephony.ims.ImsMmTelManager")
                val createMethod = mmTelClass.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
                val mmTelManager = createMethod.invoke(null, subId)
                val isAvailableMethod = mmTelClass.getMethod(
                    "isAvailable",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                // Voice over LTE (tech 0)
                val volte = isAvailableMethod.invoke(mmTelManager, CAPABILITY_TYPE_VOICE, REGISTRATION_TECH_LTE) as Boolean
                result.putBoolean(BUNDLE_VOLTE, volte)

                // Voice over IWLAN / WiFi Calling (tech 1)
                val vowifi = isAvailableMethod.invoke(mmTelManager, CAPABILITY_TYPE_VOICE, REGISTRATION_TECH_IWLAN) as Boolean
                result.putBoolean(BUNDLE_VOWIFI, vowifi)

                // Voice over NR / 5G Voice (tech 2)
                val vonr = isAvailableMethod.invoke(mmTelManager, CAPABILITY_TYPE_VOICE, REGISTRATION_TECH_NR) as Boolean
                result.putBoolean(BUNDLE_VONR, vonr)

                // Video over LTE (tech 0)
                val vt = isAvailableMethod.invoke(mmTelManager, CAPABILITY_TYPE_VIDEO, REGISTRATION_TECH_LTE) as Boolean
                result.putBoolean(BUNDLE_VT, vt)
            } catch (t: Throwable) {
                Log.w(TAG, "ImsMmTelManager capabilities check note: ${t.message}")
            }

            // 3. Query 5G NR NSA / SA status via TelephonyManager ServiceState
            try {
                val tm = context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(subId)
                val ss = tm.serviceState
                val nrRegInfo = ss?.networkRegistrationInfoList?.firstOrNull {
                    it.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN &&
                        (it.domain and NetworkRegistrationInfo.DOMAIN_PS) != 0
                }
                val nrState = if (nrRegInfo != null) {
                    val getNrStateMethod = nrRegInfo.javaClass.getMethod("getNrState")
                    getNrStateMethod.invoke(nrRegInfo) as Int
                } else {
                    NetworkRegistrationInfo.NR_STATE_NONE
                }
                result.putBoolean(
                    BUNDLE_NR_NSA,
                    nrState == NetworkRegistrationInfo.NR_STATE_CONNECTED ||
                        nrState == NetworkRegistrationInfo.NR_STATE_NOT_RESTRICTED
                )
                result.putBoolean(
                    BUNDLE_NR_SA,
                    nrRegInfo?.accessNetworkTechnology == TelephonyManager.NETWORK_TYPE_NR
                )
            } catch (t: Throwable) {
                Log.w(TAG, "5G NR status check note: ${t.message}")
            }
        }

        if (failure != null) {
            Log.e(TAG, "read IMS capabilities failed", failure)
            result.putString(BUNDLE_RESULT_MSG, failure.toPrivilegedErrorMessage())
        }
        finish(Activity.RESULT_OK, result)
    }

    companion object {
        const val TAG = "ImsCapabilityReader"
        const val BUNDLE_SELECT_SIM_ID = "select_sim_id"
        const val BUNDLE_IMS_REGISTERED = "ims_registered"
        const val BUNDLE_VOLTE = "volte"
        const val BUNDLE_VOWIFI = "vowifi"
        const val BUNDLE_VONR = "vonr"
        const val BUNDLE_VT = "vt"
        const val BUNDLE_NR_NSA = "nr_nsa"
        const val BUNDLE_NR_SA = "nr_sa"
        const val BUNDLE_RESULT_MSG = "result_msg"

        private const val REGISTRATION_TECH_LTE = 0
        private const val REGISTRATION_TECH_IWLAN = 1
        private const val REGISTRATION_TECH_NR = 2

        private const val CAPABILITY_TYPE_VOICE = 1
        private const val CAPABILITY_TYPE_VIDEO = 2
    }
}
