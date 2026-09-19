package com.autoroid.app.feature.freezer.model

data class FrozenApp(
    val packageName: String,
    val appLabel: String,
    val isSuspended: Boolean = false,
    val autoFreezeOnScreenOff: Boolean = false,
    val isSystemApp: Boolean = false
)
