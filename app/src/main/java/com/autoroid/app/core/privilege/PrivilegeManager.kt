package com.autoroid.app.core.privilege

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PrivilegeManager(private val context: Context) {

    private val rootEngine = RootEngine()
    private val shizukuEngine = ShizukuEngine()
    private val mutex = Mutex()

    private val _currentLevel = MutableStateFlow(PrivilegeLevel.NONE)
    val currentLevel: StateFlow<PrivilegeLevel> = _currentLevel.asStateFlow()

    private val _activeEngine = MutableStateFlow<PrivilegeEngine?>(null)
    val activeEngine: StateFlow<PrivilegeEngine?> = _activeEngine.asStateFlow()

    suspend fun refresh(): PrivilegeLevel = mutex.withLock {
        // Priority 1: Check Root (KernelSU / Magisk / APatch)
        if (rootEngine.isAvailable()) {
            _activeEngine.value = rootEngine
            _currentLevel.value = PrivilegeLevel.ROOT
            return@withLock PrivilegeLevel.ROOT
        }

        // Priority 2: Check Shizuku
        if (shizukuEngine.isAvailable()) {
            _activeEngine.value = shizukuEngine
            _currentLevel.value = PrivilegeLevel.SHIZUKU
            return@withLock PrivilegeLevel.SHIZUKU
        }

        _activeEngine.value = null
        _currentLevel.value = PrivilegeLevel.NONE
        PrivilegeLevel.NONE
    }

    suspend fun executeElevated(command: String): CommandResult {
        val engine = _activeEngine.value ?: run {
            // Try refreshing dynamically in case permission was just granted
            refresh()
            _activeEngine.value
        }

        return if (engine != null) {
            engine.execute(command)
        } else {
            CommandResult(-1, "", "No active elevated privilege engine (neither Root nor Shizuku available)")
        }
    }
}
