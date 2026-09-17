package com.autoroid.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CyberCyan = Color(0xFF00E5FF)
val DeepBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF151922)
val CardSurface = Color(0xFF1E232E)
val CardBorder = Color(0xFF2C3240)
val TextPrimary = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF8B949E)
val NeonGreen = Color(0xFF2EE59D)
val NeonRed = Color(0xFFFF5252)
val AmberWarn = Color(0xFFFFB300)

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    error = NeonRed,
    onError = Color.White,
    background = DeepBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun AutoroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
