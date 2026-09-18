package com.autoroid.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsCell
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class AutoroidNavTab(
    val title: String,
    val icon: ImageVector,
    val subtitle: String
) {
    IMS(
        title = "IMS & Telephony",
        icon = Icons.Default.SettingsCell,
        subtitle = "VoLTE, VoWiFi, 5G & SIM Data"
    ),
    SHIELD(
        title = "Bank Shield",
        icon = Icons.Default.Security,
        subtitle = "Anti-Detection Accessibility Protection"
    ),
    WORKFLOWS(
        title = "Automations",
        icon = Icons.Default.PlayArrow,
        subtitle = "Dynamic Macros & Screen Inspector"
    ),
    TERMINAL(
        title = "Console",
        icon = Icons.Default.Terminal,
        subtitle = "Elevated Power Shell & Diagnostic Logs"
    )
}
