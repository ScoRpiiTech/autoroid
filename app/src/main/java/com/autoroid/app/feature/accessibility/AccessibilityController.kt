package com.autoroid.app.feature.accessibility

import android.content.Context
import android.content.SharedPreferences
import com.autoroid.app.core.privilege.CommandResult
import com.autoroid.app.core.privilege.PrivilegeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccessibilityController(
    private val context: Context,
    private val privilegeManager: PrivilegeManager
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autoroid_bank_mode", Context.MODE_PRIVATE)

    private val _isBankModeActive = MutableStateFlow(false)
    val isBankModeActive: StateFlow<Boolean> = _isBankModeActive.asStateFlow()

    private val _activeServicesList = MutableStateFlow<List<String>>(emptyList())
    val activeServicesList: StateFlow<List<String>> = _activeServicesList.asStateFlow()

    companion object {
        private const val KEY_SAVED_SERVICES = "saved_accessibility_services"
        private const val CMD_GET_SERVICES = "settings get secure enabled_accessibility_services"
        private const val CMD_SET_SERVICES = "settings put secure enabled_accessibility_services"
        private const val CMD_SET_ENABLED = "settings put secure accessibility_enabled"
    }

    suspend fun refreshState() {
        val result = privilegeManager.executeElevated(CMD_GET_SERVICES)
        if (result.isSuccess) {
            val raw = result.stdout.trim()
            val clean = if (raw == "null") "" else raw
            if (clean.isBlank()) {
                // No services running -> Bank mode is active (protected)
                _isBankModeActive.value = true
                _activeServicesList.value = emptyList()
            } else {
                _isBankModeActive.value = false
                _activeServicesList.value = clean.split(":").filter { it.isNotBlank() }
            }
        }
    }

    /**
     * Activates Bank Mode:
     * 1. Backs up current enabled accessibility services list
     * 2. Clears all accessibility services
     * 3. Disables accessibility master switch to bypass bank app anti-automation detections
     */
    suspend fun enableBankMode(): CommandResult {
        // Query current services before clearing
        val queryResult = privilegeManager.executeElevated(CMD_GET_SERVICES)
        val current = queryResult.stdout.trim()
        if (current.isNotBlank() && current != "null") {
            prefs.edit().putString(KEY_SAVED_SERVICES, current).apply()
        }

        // Clear enabled services
        val clearResult = privilegeManager.executeElevated("$CMD_SET_SERVICES \"\"")
        privilegeManager.executeElevated("$CMD_SET_ENABLED 0")

        refreshState()
        return clearResult
    }

    /**
     * Deactivates Bank Mode:
     * Restores previously saved accessibility services.
     */
    suspend fun disableBankMode(): CommandResult {
        val saved = prefs.getString(KEY_SAVED_SERVICES, "") ?: ""
        val result = if (saved.isNotBlank()) {
            val restoreResult = privilegeManager.executeElevated("$CMD_SET_SERVICES \"$saved\"")
            privilegeManager.executeElevated("$CMD_SET_ENABLED 1")
            restoreResult
        } else {
            CommandResult(0, "No previous services to restore", "")
        }

        refreshState()
        return result
    }

    suspend fun toggleBankMode(): CommandResult {
        return if (_isBankModeActive.value) {
            disableBankMode()
        } else {
            enableBankMode()
        }
    }
}
