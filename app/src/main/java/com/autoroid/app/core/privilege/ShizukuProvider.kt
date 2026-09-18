package com.autoroid.app.core.privilege

import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuProvider

/**
 * Custom Shizuku ContentProvider that initializes hidden API exemptions
 * at the earliest possible stage in the Android process lifecycle (before Application.onCreate).
 */
class ShizukuProvider : ShizukuProvider() {
    override fun onCreate(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("")
            } catch (_: Throwable) {
            }
        }
        return super.onCreate()
    }
}
