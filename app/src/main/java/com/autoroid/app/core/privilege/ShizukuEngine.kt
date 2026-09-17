package com.autoroid.app.core.privilege

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku

class ShizukuEngine : PrivilegeEngine {

    override val level: PrivilegeLevel = PrivilegeLevel.SHIZUKU

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder()) {
                return@withContext false
            }
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    override suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder()) {
                return@withContext CommandResult(-1, "", "Shizuku service is not running or binder dead")
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return@withContext CommandResult(-2, "", "Shizuku permission not granted")
            }

            val binder = Shizuku.getBinder()
                ?: return@withContext CommandResult(-1, "", "Shizuku binder is null")
            val service = IShizukuService.Stub.asInterface(binder)
                ?: return@withContext CommandResult(-1, "", "Unable to obtain IShizukuService interface")

            val remoteProcess = service.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )

            val stdout = ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream).bufferedReader().use { it.readText() }.trim()
            val stderr = ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.errorStream).bufferedReader().use { it.readText() }.trim()
            val exitCode = remoteProcess.waitFor()

            CommandResult(exitCode, stdout, stderr)
        } catch (e: Throwable) {
            CommandResult(-1, "", e.localizedMessage ?: "Unknown Shizuku execution failure")
        }
    }
}

