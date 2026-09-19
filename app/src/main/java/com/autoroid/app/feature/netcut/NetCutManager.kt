package com.autoroid.app.feature.netcut

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.netcut.model.NetCutApp
import com.autoroid.app.feature.netcut.repository.NetCutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class NetCutManager(
    private val context: Context,
    private val repository: NetCutRepository,
    private val privilegeManager: PrivilegeManager
) {
    private val pm: PackageManager = context.packageManager

    private val _apps = MutableStateFlow<List<NetCutApp>>(emptyList())
    val apps: StateFlow<List<NetCutApp>> = _apps.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    suspend fun refreshApps() = withContext(Dispatchers.IO) {
        _isBusy.value = true
        try {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val blockedSet = repository.blockedPackages.value

            val list = installed.mapNotNull { appInfo ->
                if (appInfo.packageName == context.packageName) return@mapNotNull null

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    appInfo.packageName
                }

                NetCutApp(
                    packageName = appInfo.packageName,
                    appLabel = label,
                    uid = appInfo.uid,
                    isInternetBlocked = blockedSet.contains(appInfo.packageName),
                    isSystemApp = isSystem
                )
            }.sortedWith(compareBy({ !it.isInternetBlocked }, { it.appLabel.lowercase() }))

            _apps.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Failed refreshing apps: ${e.message}", e)
        } finally {
            _isBusy.value = false
        }
    }

    suspend fun setInternetBlocked(packageName: String, blocked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val uid = try {
            pm.getPackageUid(packageName, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot resolve UID for $packageName: ${e.message}")
            return@withContext false
        }

        Log.i(TAG, "Setting NetCut internet blocked=$blocked for $packageName (UID $uid)")
        val level = privilegeManager.currentLevel.value

        val success = if (level == PrivilegeLevel.ROOT) {
            applyIptablesRule(uid, blocked)
        } else {
            applyNetpolicyRule(packageName, uid, blocked)
        }

        if (success) {
            repository.setBlocked(packageName, blocked)
            val current = _apps.value
            _apps.value = current.map {
                if (it.packageName == packageName) it.copy(isInternetBlocked = blocked) else it
            }
        }
        success
    }

    private suspend fun applyIptablesRule(uid: Int, blocked: Boolean): Boolean {
        return if (blocked) {
            privilegeManager.executeElevated("iptables -I OUTPUT -m owner --uid-owner $uid -j DROP")
            privilegeManager.executeElevated("ip6tables -I OUTPUT -m owner --uid-owner $uid -j DROP").isSuccess
        } else {
            privilegeManager.executeElevated("iptables -D OUTPUT -m owner --uid-owner $uid -j DROP")
            privilegeManager.executeElevated("ip6tables -D OUTPUT -m owner --uid-owner $uid -j DROP").isSuccess
        }
    }

    private suspend fun applyNetpolicyRule(packageName: String, uid: Int, blocked: Boolean): Boolean {
        return if (blocked) {
            privilegeManager.executeElevated("cmd netpolicy add restrict-background-blacklist $uid")
            privilegeManager.executeElevated("cmd netpolicy set-uid-policy $uid 1")
            privilegeManager.executeElevated("cmd appops set $packageName RUN_IN_BACKGROUND ignore")
            privilegeManager.executeElevated("cmd appops set $packageName RUN_ANY_IN_BACKGROUND ignore").isSuccess
        } else {
            privilegeManager.executeElevated("cmd netpolicy remove restrict-background-blacklist $uid")
            privilegeManager.executeElevated("cmd netpolicy set-uid-policy $uid 0")
            privilegeManager.executeElevated("cmd appops set $packageName RUN_IN_BACKGROUND allow")
            privilegeManager.executeElevated("cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow").isSuccess
        }
    }

    suspend fun onBoot() = withContext(Dispatchers.IO) {
        val blocked = repository.blockedPackages.value
        if (blocked.isEmpty()) return@withContext

        Log.i(TAG, "Restoring NetCut firewall rules for ${blocked.size} package(s) on boot.")
        blocked.forEach { pkg ->
            try {
                val uid = pm.getPackageUid(pkg, 0)
                if (privilegeManager.currentLevel.value == PrivilegeLevel.ROOT) {
                    applyIptablesRule(uid, true)
                } else {
                    applyNetpolicyRule(pkg, uid, true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed re-applying NetCut for $pkg: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "NetCutManager"
    }
}
