package com.autoroid.app.core.privilege

interface PrivilegeEngine {
    val level: PrivilegeLevel
    suspend fun isAvailable(): Boolean
    suspend fun execute(command: String): CommandResult
}
