package com.autoroid.app.feature.netcut.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetCutRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _blockedPackages = MutableStateFlow<Set<String>>(loadBlockedPackages())
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    private fun loadBlockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet()
    }

    fun setBlocked(packageName: String, blocked: Boolean) {
        val current = _blockedPackages.value.toMutableSet()
        if (blocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_BLOCKED, current).apply()
        _blockedPackages.value = current
    }

    fun isBlocked(packageName: String): Boolean {
        return _blockedPackages.value.contains(packageName)
    }

    companion object {
        private const val PREFS_NAME = "autoroid_netcut"
        private const val KEY_BLOCKED = "blocked_packages"
    }
}
