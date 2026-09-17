package com.autoroid.app.feature.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.update.model.UpdateInfo
import com.autoroid.app.feature.update.model.UpdateState
import com.autoroid.app.feature.update.model.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import moe.shizuku.server.IShizukuService
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(
    private val context: Context,
    private val privilegeManager: PrivilegeManager
) {
    private val _updateStatus = MutableStateFlow(UpdateStatus())
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    companion object {
        const val GITHUB_OWNER = "ScoRpiiTech"
        const val GITHUB_REPO = "autoroid"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    val currentVersion: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.2.3"
        } catch (_: Exception) {
            "1.2.3"
        }

    fun isApkDownloaded(apkFile: File, targetTag: String): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0) return false
        return try {
            val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            val targetClean = targetTag.trim().removePrefix("v").removePrefix("V")
            val archiveClean = archiveInfo?.versionName?.trim()?.removePrefix("v")?.removePrefix("V")
            archiveClean != null && archiveClean == targetClean
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus(state = UpdateState.CHECKING, message = "Checking GitHub for updates...")
        try {
            val url = URL(API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Autoroid-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode != 200) {
                val errorMsg = when (conn.responseCode) {
                    404 -> "No GitHub releases found on repository."
                    403 -> "GitHub API rate limit reached. Please wait a moment."
                    else -> "GitHub API returned HTTP ${conn.responseCode}"
                }
                _updateStatus.value = UpdateStatus(
                    state = UpdateState.ERROR,
                    message = errorMsg
                )
                return@withContext null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "No release notes provided.")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val updatesDir = File(context.cacheDir, "updates")
            val apkFile = File(updatesDir, "autoroid-update.apk")
            val isDownloaded = isApkDownloaded(apkFile, tagName)

            val isNewer = isNewerVersion(tagName, currentVersion)
            val info = UpdateInfo(
                latestVersion = tagName,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                hasUpdate = isNewer && downloadUrl.isNotBlank(),
                publishedAt = publishedAt,
                isDownloaded = isDownloaded
            )

            if (info.hasUpdate) {
                _updateStatus.value = UpdateStatus(
                    state = UpdateState.AVAILABLE,
                    updateInfo = info,
                    message = if (isDownloaded) "Update $tagName is downloaded and ready to install!" else "New version $tagName available!"
                )
            } else {
                // If already on latest, clean up any old cached update APK
                if (apkFile.exists()) {
                    try { apkFile.delete() } catch (_: Exception) {}
                }
                _updateStatus.value = UpdateStatus(
                    state = UpdateState.UP_TO_DATE,
                    updateInfo = info,
                    message = "Autoroid is up to date ($currentVersion)"
                )
            }

            info
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus(
                state = UpdateState.ERROR,
                message = "Failed to check update: ${e.localizedMessage}"
            )
            null
        }
    }

    suspend fun downloadAndInstall(updateInfo: UpdateInfo) = withContext(Dispatchers.IO) {
        if (updateInfo.downloadUrl.isBlank()) {
            _updateStatus.value = UpdateStatus(state = UpdateState.ERROR, message = "Invalid download URL")
            return@withContext
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "autoroid-update.apk")

        // If package is already downloaded and verified, skip downloading!
        if (isApkDownloaded(apkFile, updateInfo.latestVersion)) {
            _updateStatus.value = UpdateStatus(
                state = UpdateState.INSTALLING,
                updateInfo = updateInfo.copy(isDownloaded = true),
                progressPercent = 100,
                message = "Package already downloaded. Installing update..."
            )
            installApk(apkFile, updateInfo)
            return@withContext
        }

        _updateStatus.value = UpdateStatus(
            state = UpdateState.DOWNLOADING,
            updateInfo = updateInfo,
            progressPercent = 0,
            message = "Starting download..."
        )

        try {
            val url = URL(updateInfo.downloadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Autoroid-App")
                connectTimeout = 15000
                readTimeout = 30000
            }

            val totalLength = conn.contentLength
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var lastPercent = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalLength > 0) {
                            val percent = ((downloadedBytes * 100) / totalLength).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _updateStatus.value = UpdateStatus(
                                    state = UpdateState.DOWNLOADING,
                                    updateInfo = updateInfo,
                                    progressPercent = percent,
                                    message = "Downloading: $percent%"
                                )
                            }
                        }
                    }
                }
            }

            // Installation Phase
            _updateStatus.value = UpdateStatus(
                state = UpdateState.INSTALLING,
                updateInfo = updateInfo.copy(isDownloaded = true),
                progressPercent = 100,
                message = "Installing update..."
            )

            installApk(apkFile, updateInfo)

        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus(
                state = UpdateState.ERROR,
                message = "Download failed: ${e.localizedMessage}"
            )
        }
    }

    private suspend fun performElevatedInstall(apkFile: File): Boolean {
        val currentLevel = privilegeManager.currentLevel.value

        // Priority 1A: Direct root execution (UID 0 can access /data/user/0 directly)
        if (currentLevel == PrivilegeLevel.ROOT) {
            val res = privilegeManager.executeElevated("pm install -r -d ${apkFile.absolutePath}")
            if (res.isSuccess && !res.stdout.contains("Failure", ignoreCase = true) && !res.stderr.contains("Failure", ignoreCase = true)) {
                return true
            }
        }

        // Priority 1B: Shizuku IPC stream execution (UID 2000 shell pipes to /data/local/tmp)
        if (currentLevel == PrivilegeLevel.SHIZUKU || Shizuku.pingBinder()) {
            try {
                if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    val binder = Shizuku.getBinder()
                    if (binder != null) {
                        val service = IShizukuService.Stub.asInterface(binder)
                        val tmpApk = "/data/local/tmp/autoroid-update.apk"

                        // Stream APK to /data/local/tmp
                        val catProcess = service.newProcess(
                            arrayOf("sh", "-c", "cat > $tmpApk"),
                            null,
                            null
                        )
                        ParcelFileDescriptor.AutoCloseOutputStream(catProcess.outputStream).use { out ->
                            apkFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        val catExit = catProcess.waitFor()
                        if (catExit == 0) {
                            val installProcess = service.newProcess(
                                arrayOf("sh", "-c", "pm install -r -d $tmpApk && rm -f $tmpApk"),
                                null,
                                null
                            )
                            val stdout = ParcelFileDescriptor.AutoCloseInputStream(installProcess.inputStream)
                                .bufferedReader().use { it.readText() }
                            val exitCode = installProcess.waitFor()
                            if (exitCode == 0 && stdout.contains("Success", ignoreCase = true)) {
                                return true
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall back
            }
        }

        // Priority 1C: Fallback elevated command
        val fallback = privilegeManager.executeElevated("pm install -r -d ${apkFile.absolutePath}")
        return fallback.isSuccess && fallback.stdout.contains("Success", ignoreCase = true)
    }

    private suspend fun installApk(apkFile: File, updateInfo: UpdateInfo) {
        val installSuccess = performElevatedInstall(apkFile)

        if (installSuccess) {
            _updateStatus.value = UpdateStatus(
                state = UpdateState.UP_TO_DATE,
                message = "Update installed successfully! Restarting..."
            )
            // Restart application
            privilegeManager.executeElevated("am start -n ${context.packageName}/.ui.MainActivity")
            return
        }

        // Priority 2: Standard Android PackageInstaller Fallback
        withContext(Dispatchers.Main) {
            try {
                apkFile.setReadable(true, false)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                _updateStatus.value = UpdateStatus(
                    state = UpdateState.AVAILABLE,
                    updateInfo = updateInfo.copy(isDownloaded = true),
                    message = "System installer opened. Please confirm the installation prompt."
                )
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus(
                    state = UpdateState.ERROR,
                    message = "Installer launch failed: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteClean = remoteTag.trim().removePrefix("v").removePrefix("V")
        val currentClean = currentVersion.trim().removePrefix("v").removePrefix("V")

        if (remoteClean.isBlank() || currentClean.isBlank()) return false
        if (remoteClean == currentClean) return false

        val remoteParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
