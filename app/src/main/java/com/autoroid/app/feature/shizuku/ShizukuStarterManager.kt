package com.autoroid.app.feature.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.core.privilege.PrivilegeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ShizukuStarterManager(
    private val context: Context,
    private val privilegeManager: PrivilegeManager
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _wirelessAdbPort = MutableStateFlow<Int?>(null)
    val wirelessAdbPort: StateFlow<Int?> = _wirelessAdbPort.asStateFlow()

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun refreshStatus() = withContext(Dispatchers.IO) {
        val running = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
        _isRunning.value = running

        // Check wireless ADB port
        try {
            val portStr = privilegeManager.executeElevated("getprop service.adb.tcp.port").stdout.trim()
            val port = portStr.toIntOrNull()
            _wirelessAdbPort.value = if (port != null && port > 0) port else null
        } catch (e: Exception) {
            _wirelessAdbPort.value = null
        }
    }

    suspend fun startShizuku(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Attempting to start Shizuku daemon...")

        // Method 1: If Root is available, execute Shizuku's starter script directly
        val starterPaths = listOf(
            "/data/user_de/0/moe.shizuku.privileged.api/bin/shizuku_starter",
            "/data/user/0/moe.shizuku.privileged.api/bin/shizuku_starter",
            "sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
            "sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh"
        )

        for (cmd in starterPaths) {
            val res = privilegeManager.executeElevated(cmd)
            if (res.isSuccess) {
                Log.i(TAG, "Shizuku starter command succeeded via: $cmd")
                kotlinx.coroutines.delay(1000)
                refreshStatus()
                if (_isRunning.value) return@withContext true
            }
        }

        // Method 2: Launch Shizuku app if installed
        openShizukuApp()
        false
    }

    fun openShizukuApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    companion object {
        private const val TAG = "ShizukuStarterManager"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
