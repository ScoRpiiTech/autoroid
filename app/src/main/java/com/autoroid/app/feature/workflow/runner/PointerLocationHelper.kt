package com.autoroid.app.feature.workflow.runner

import com.autoroid.app.core.privilege.PrivilegeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PointerLocationHelper(private val privilegeManager: PrivilegeManager) {

    private val _isPointerLocationActive = MutableStateFlow(false)
    val isPointerLocationActive: StateFlow<Boolean> = _isPointerLocationActive.asStateFlow()

    companion object {
        private const val CMD_GET = "settings get system pointer_location"
        private const val CMD_SET = "settings put system pointer_location"
    }

    suspend fun refreshState() {
        val res = privilegeManager.executeElevated(CMD_GET)
        if (res.isSuccess) {
            _isPointerLocationActive.value = res.stdout.trim() == "1"
        }
    }

    suspend fun toggle(): Boolean {
        val willEnable = !_isPointerLocationActive.value
        val res = privilegeManager.executeElevated("$CMD_SET ${if (willEnable) "1" else "0"}")
        if (res.isSuccess) {
            _isPointerLocationActive.value = willEnable
        }
        return _isPointerLocationActive.value
    }
}
