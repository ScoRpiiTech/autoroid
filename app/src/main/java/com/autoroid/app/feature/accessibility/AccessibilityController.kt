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

    private val _isBankModeEngaged = MutableStateFlow(false)
    val isBankModeActive: StateFlow<Boolean> = _isBankModeEngaged.asStateFlow()

    private val _activeServicesList = MutableStateFlow<List<String>>(emptyList())
    val activeServicesList: StateFlow<List<String>> = _activeServicesList.asStateFlow()

    private val _pausedServicesList = MutableStateFlow<List<String>>(emptyList())
    val pausedServicesList: StateFlow<List<String>> = _pausedServicesList.asStateFlow()

    companion object {
        private const val KEY_SAVED_SERVICES = "saved_accessibility_services"
        private const val KEY_BANK_MODE_ENGAGED = "is_bank_mode_engaged"
        private const val CMD_GET_SERVICES = "settings get secure enabled_accessibility_services"
        private const val CMD_SET_SERVICES = "settings put secure enabled_accessibility_services"
        private const val CMD_SET_ENABLED = "settings put secure accessibility_enabled"
    }

    init {
        val saved = prefs.getString(KEY_SAVED_SERVICES, "") ?: ""
        _pausedServicesList.value = saved.split(":").filter { it.isNotBlank() }
        _isBankModeEngaged.value = prefs.getBoolean(KEY_BANK_MODE_ENGAGED, false) && _pausedServicesList.value.isNotEmpty()
    }

    suspend fun refreshState() {
        val saved = prefs.getString(KEY_SAVED_SERVICES, "") ?: ""
        val savedList = saved.split(":").filter { it.isNotBlank() }
        _pausedServicesList.value = savedList

        val result = privilegeManager.executeElevated(CMD_GET_SERVICES)
        if (result.isSuccess) {
            val raw = result.stdout.trim()
            val clean = if (raw == "null") "" else raw
            val currentServices = if (clean.isBlank()) emptyList() else clean.split(":").filter { it.isNotBlank() }
            _activeServicesList.value = currentServices

            if (currentServices.isNotEmpty()) {
                // Services are actively running in Android
                _isBankModeEngaged.value = false
                prefs.edit().putBoolean(KEY_BANK_MODE_ENGAGED, false).apply()
            } else {
                // No services running currently in Android
                // Only consider bank mode engaged if Autoroid actively paused them
                val wasEngaged = prefs.getBoolean(KEY_BANK_MODE_ENGAGED, false) && savedList.isNotEmpty()
                _isBankModeEngaged.value = wasEngaged
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
        val clean = if (current == "null") "" else current
        val currentList = if (clean.isBlank()) emptyList() else clean.split(":").filter { it.isNotBlank() }

        if (currentList.isEmpty()) {
            _isBankModeEngaged.value = false
            prefs.edit().putBoolean(KEY_BANK_MODE_ENGAGED, false).apply()
            return CommandResult(0, "No active accessibility services to pause", "")
        }

        // Save active services
        prefs.edit()
            .putString(KEY_SAVED_SERVICES, clean)
            .putBoolean(KEY_BANK_MODE_ENGAGED, true)
            .apply()
        _pausedServicesList.value = currentList

        // Clear enabled services
        val clearResult = privilegeManager.executeElevated("$CMD_SET_SERVICES \"\"")
        privilegeManager.executeElevated("$CMD_SET_ENABLED 0")

        _isBankModeEngaged.value = true
        _activeServicesList.value = emptyList()

        return clearResult
    }

    /**
     * Deactivates Bank Mode:
     * Restores previously saved accessibility services.
     */
    suspend fun disableBankMode(): CommandResult {
        val saved = prefs.getString(KEY_SAVED_SERVICES, "") ?: ""
        val savedList = saved.split(":").filter { it.isNotBlank() }

        val result = if (savedList.isNotEmpty()) {
            val restoreResult = privilegeManager.executeElevated("$CMD_SET_SERVICES \"$saved\"")
            privilegeManager.executeElevated("$CMD_SET_ENABLED 1")
            restoreResult
        } else {
            CommandResult(0, "No previous services to restore", "")
        }

        prefs.edit()
            .putString(KEY_SAVED_SERVICES, "")
            .putBoolean(KEY_BANK_MODE_ENGAGED, false)
            .apply()

        _isBankModeEngaged.value = false
        _pausedServicesList.value = emptyList()

        refreshState()
        return result
    }

    suspend fun toggleBankMode(): CommandResult {
        return if (_isBankModeEngaged.value) {
            disableBankMode()
        } else {
            enableBankMode()
        }
    }
}
