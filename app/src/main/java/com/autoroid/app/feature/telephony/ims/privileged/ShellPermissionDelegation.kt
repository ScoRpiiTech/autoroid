package com.autoroid.app.feature.telephony.ims.privileged

import android.app.IActivityManager
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import android.os.ServiceManager
import android.system.Os
import android.util.Log
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.InvocationTargetException

private const val TAG = "ShellPermission"
private const val ANDROID_17_SDK = 37

fun IActivityManager.stopDelegateShellPermissionIdentityCompat(uid: Int = Os.getuid()) {
    if (Build.VERSION.SDK_INT >= ANDROID_17_SDK) {
        try {
            stopDelegateShellPermissionIdentity(uid)
            return
        } catch (e: Throwable) {
            Log.w(TAG, "stopDelegateShellPermissionIdentity(uid) fallback", e)
        }
        try {
            stopDelegateShellPermissionIdentity()
        } catch (e: Throwable) {
            Log.w(TAG, "stopDelegateShellPermissionIdentity() failed", e)
        }
        return
    }

    try {
        stopDelegateShellPermissionIdentity()
    } catch (e: Throwable) {
        Log.w(TAG, "stopDelegateShellPermissionIdentity() fallback to uid", e)
        try {
            stopDelegateShellPermissionIdentity(uid)
        } catch (e2: Throwable) {
            Log.w(TAG, "stopDelegateShellPermissionIdentity(uid) failed", e2)
        }
    }
}

/**
 * Executes a privileged block within an active Instrumentation session with shell permission delegation.
 */
fun Instrumentation.runWithShellPermissionDelegation(
    tag: String,
    block: () -> Unit
): Throwable? {
    var activityManager: IActivityManager? = null
    var delegationStarted = false
    var failure: Throwable? = null

    try {
        val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
            ?: error("activity service unavailable")
        activityManager = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))

        Log.i(tag, "Starting shell permission identity delegation for UID ${Os.getuid()}")
        activityManager.startDelegateShellPermissionIdentity(Os.getuid(), null)
        delegationStarted = true
        block()
    } catch (t: Throwable) {
        failure = t
    } finally {
        if (delegationStarted) {
            try {
                activityManager?.stopDelegateShellPermissionIdentityCompat()
                Log.i(tag, "Stopped shell permission delegation")
            } catch (cleanup: Throwable) {
                Log.e(tag, "Failed to stop shell permission delegation", cleanup)
                if (failure == null) {
                    failure = cleanup
                } else {
                    failure.addSuppressed(cleanup)
                }
            }
        }
    }
    return failure
}

fun Throwable.privilegedRootCause(): Throwable {
    var current = this
    val visited = mutableSetOf<Throwable>()
    while (visited.add(current)) {
        current = when (current) {
            is InvocationTargetException -> current.targetException ?: return current
            else -> current.cause ?: return current
        }
    }
    return current
}

fun Throwable.toPrivilegedErrorMessage(): String {
    val root = privilegedRootCause()
    val name = root.javaClass.simpleName.ifBlank { root.javaClass.name }
    return root.message?.takeIf { it.isNotBlank() }?.let { "$name: $it" } ?: name
}

fun isCarrierConfigPermissionError(message: String): Boolean {
    return message.contains("SecurityException", ignoreCase = true) ||
            message.contains("android.permission.MODIFY_PHONE_STATE", ignoreCase = true) ||
            message.contains("No permission to write to carrier config", ignoreCase = true) ||
            message.contains("overrideConfig cannot be invoked", ignoreCase = true)
}
