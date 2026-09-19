package com.autoroid.app.feature.freezer

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.freezer.model.FrozenApp
import com.autoroid.app.feature.freezer.repository.AppFreezerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppFreezerManager(
    private val context: Context,
    private val repository: AppFreezerRepository,
    private val privilegeManager: PrivilegeManager
) {
    private val pm: PackageManager = context.packageManager

    private val _installedApps = MutableStateFlow<List<FrozenApp>>(emptyList())
    val installedApps: StateFlow<List<FrozenApp>> = _installedApps.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    suspend fun refreshApps() = withContext(Dispatchers.IO) {
        _isBusy.value = true
        try {
            val flags = PackageManager.GET_META_DATA
            val apps = pm.getInstalledApplications(flags)
            val autoFreezeSet = repository.autoFreezePackages.value

            val list = apps.mapNotNull { appInfo ->
                if (appInfo.packageName == context.packageName) return@mapNotNull null

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    appInfo.packageName
                }

                val isSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        pm.isPackageSuspended(appInfo.packageName)
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }

                FrozenApp(
                    packageName = appInfo.packageName,
                    appLabel = label,
                    isSuspended = isSuspended,
                    autoFreezeOnScreenOff = autoFreezeSet.contains(appInfo.packageName),
                    isSystemApp = isSystem
                )
            }.sortedWith(compareBy({ !it.isSuspended }, { it.appLabel.lowercase() }))

            _installedApps.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Failed refreshing apps: ${e.message}", e)
        } finally {
            _isBusy.value = false
        }
    }

    suspend fun freezeApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Freezing package: $packageName")
        val suspendRes = privilegeManager.executeElevated("pm suspend $packageName")
        val stopRes = privilegeManager.executeElevated("am force-stop $packageName")
        val success = suspendRes.isSuccess || stopRes.isSuccess

        updateLocalAppState(packageName, isSuspended = true)
        success
    }

    suspend fun unfreezeApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Unfreezing package: $packageName")
        val res = privilegeManager.executeElevated("pm unsuspend $packageName")
        updateLocalAppState(packageName, isSuspended = false)
        res.isSuccess
    }

    fun setAutoFreeze(packageName: String, enabled: Boolean) {
        repository.setAutoFreeze(packageName, enabled)
        val currentList = _installedApps.value
        _installedApps.value = currentList.map {
            if (it.packageName == packageName) it.copy(autoFreezeOnScreenOff = enabled) else it
        }
    }

    suspend fun onScreenOff() = withContext(Dispatchers.IO) {
        val targets = repository.autoFreezePackages.value
        if (targets.isEmpty()) return@withContext

        Log.i(TAG, "Screen off detected: auto-freezing ${targets.size} packages.")
        targets.forEach { pkg ->
            try {
                privilegeManager.executeElevated("pm suspend $pkg")
                privilegeManager.executeElevated("am force-stop $pkg")
            } catch (e: Exception) {
                Log.w(TAG, "Auto-freeze failed for $pkg: ${e.message}")
            }
        }
        refreshApps()
    }

    suspend fun launchApp(packageName: String) = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm.isPackageSuspended(packageName)) {
            unfreezeApp(packageName)
        }
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun updateLocalAppState(packageName: String, isSuspended: Boolean) {
        val current = _installedApps.value
        _installedApps.value = current.map {
            if (it.packageName == packageName) it.copy(isSuspended = isSuspended) else it
        }
    }

    companion object {
        private const val TAG = "AppFreezerManager"
    }
}
