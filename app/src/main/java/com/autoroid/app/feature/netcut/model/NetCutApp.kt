package com.autoroid.app.feature.netcut.model

data class NetCutApp(
    val packageName: String,
    val appLabel: String,
    val uid: Int,
    val isInternetBlocked: Boolean = false,
    val isSystemApp: Boolean = false
)
