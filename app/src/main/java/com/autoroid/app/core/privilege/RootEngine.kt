package com.autoroid.app.core.privilege

import com.autoroid.app.core.native.NativeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class RootEngine : PrivilegeEngine {

    override val level: PrivilegeLevel = PrivilegeLevel.ROOT

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!NativeEngine.isDirectRootAvailable()) {
                return@withContext false
            }
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            exitCode == 0 && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val stderr = process.errorStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.waitFor()
            CommandResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            CommandResult(-1, "", e.localizedMessage ?: "Unknown Root execution failure")
        }
    }
}
