package com.autoroid.app.feature.update.model

data class UpdateInfo(
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val hasUpdate: Boolean,
    val publishedAt: String = ""
)

enum class UpdateState {
    IDLE,
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    INSTALLING,
    UP_TO_DATE,
    ERROR
}

data class UpdateStatus(
    val state: UpdateState = UpdateState.IDLE,
    val updateInfo: UpdateInfo? = null,
    val progressPercent: Int = 0,
    val message: String = ""
)
