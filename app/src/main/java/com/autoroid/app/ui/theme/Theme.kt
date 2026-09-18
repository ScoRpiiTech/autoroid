package com.autoroid.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val CyberCyan = Color(0xFF00E5FF)
val ElectricBlue = Color(0xFF00A3FF)
val NeonGreen = Color(0xFF2EE59D)
val NeonPurple = Color(0xFFA855F7)
val NeonRed = Color(0xFFFF5252)
val AmberWarn = Color(0xFFFFB300)

val DeepBackground = Color(0xFF0A0D14)
val DarkSurface = Color(0xFF121722)
val CardSurface = Color(0xFF181F2C)
val CardBorder = Color(0xFF263043)
val GlassSurface = Color(0xCC161E2E)
val GlassBorder = Color(0x4000E5FF)
val GlassCardBorder = Color(0x332EE59D)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Gradients
val CyberCyanGradient = Brush.horizontalGradient(
    listOf(CyberCyan, ElectricBlue)
)

val NeonGreenGradient = Brush.horizontalGradient(
    listOf(NeonGreen, CyberCyan)
)

val PurpleCyanGradient = Brush.horizontalGradient(
    listOf(NeonPurple, CyberCyan)
)

val CardGlassGradient = Brush.verticalGradient(
    listOf(Color(0xFF1A2232), Color(0xFF121722))
)

val FloatingBarGradient = Brush.verticalGradient(
    listOf(Color(0xE6131A26), Color(0xF20B0F17))
)

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    tertiary = NeonPurple,
    onTertiary = Color.White,
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

