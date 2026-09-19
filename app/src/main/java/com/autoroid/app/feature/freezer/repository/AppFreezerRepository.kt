package com.autoroid.app.feature.freezer.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppFreezerRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _autoFreezePackages = MutableStateFlow<Set<String>>(loadAutoFreezePackages())
    val autoFreezePackages: StateFlow<Set<String>> = _autoFreezePackages.asStateFlow()

    private fun loadAutoFreezePackages(): Set<String> {
        return prefs.getStringSet(KEY_AUTO_FREEZE, emptySet()) ?: emptySet()
    }

    fun setAutoFreeze(packageName: String, enabled: Boolean) {
        val current = _autoFreezePackages.value.toMutableSet()
        if (enabled) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_AUTO_FREEZE, current).apply()
        _autoFreezePackages.value = current
    }

    fun isAutoFreeze(packageName: String): Boolean {
        return _autoFreezePackages.value.contains(packageName)
    }

    companion object {
        private const val PREFS_NAME = "autoroid_app_freezer"
        private const val KEY_AUTO_FREEZE = "auto_freeze_packages"
    }
}
