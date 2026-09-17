package com.autoroid.app.core.privilege

enum class PrivilegeLevel(val label: String) {
    ROOT("Root (UID 0)"),
    SHIZUKU("Shizuku (ADB UID 2000)"),
    ADB("Direct ADB Shell"),
    NONE("No Elevated Privileges")
}

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}
