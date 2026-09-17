package com.autoroid.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.autoroid.app.ui.theme.AmberWarn
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DeepBackground
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary

enum class FeatureHelpType(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val summary: String,
    val whyNeeded: String,
    val steps: List<String>,
    val tips: List<String>
) {
    ENGINE_PRIVILEGE(
        title = "Privilege Engine (Root & Shizuku)",
        subtitle = "Android Elevation Requirements",
        icon = Icons.Default.Terminal,
        accentColor = CyberCyan,
        summary = "Autoroid commands low-level Android system services that regular Google Play apps cannot touch. To operate, it requires elevated shell permissions.",
        whyNeeded = "Android blocks standard apps from modifying accessibility settings, switching SIM data lines, or injecting screen taps without user intervention.",
        steps = listOf(
            "If your device is Rooted: Grant Superuser (KernelSU, APatch, or Magisk) when prompted.",
            "If NOT Rooted: Install the free 'Shizuku' app from GitHub or Play Store.",
            "Enable Wireless Debugging in Android Developer Options and start Shizuku.",
            "Tap 'Connect Shizuku / Request Privilege' in Autoroid to authorize the binder bridge."
        ),
        tips = listOf(
            "Without Root or Shizuku, tapping buttons will fail because Android blocks the system commands.",
            "Once Shizuku is authorized, it remains connected until your phone restarts."
        )
    ),
    BANK_MODE(
        title = "Bank Mode",
        subtitle = "Accessibility Cloaking & Restoration",
        icon = Icons.Default.Security,
        accentColor = NeonGreen,
        summary = "Instantly pauses all running accessibility services so sensitive banking and enterprise apps don't block you from logging in.",
        whyNeeded = "Modern banking apps (Chase, Revolut, finance apps) scan for running accessibility services (like password autofill, Tasker, AutoInput) and refuse to launch with security warnings.",
        steps = listOf(
            "Check Status: If 'ALL CLEAR' is shown, you have 0 accessibility services and are already safe!",
            "If services are active: Tap 'ACTIVATE BANK MODE' before opening your banking or crypto app.",
            "Autoroid snapshots your running services and unbinds them from Android system settings.",
            "Open your banking app without any warnings or blocks.",
            "When finished, tap 'RESTORE SERVICES' (or use the Quick Settings notification tile) to instantly re-enable all your tools."
        ),
        tips = listOf(
            "If your phone has no accessibility services installed or enabled, it displays 'ALL CLEAR' and no action is required.",
            "Restoring services only restores the exact services that Autoroid previously paused."
        )
    ),
    SIM_SWITCHER(
        title = "Dual-SIM Data Switcher",
        subtitle = "Physical SIM & eSIM Modem Switching",
        icon = Icons.Default.SimCard,
        accentColor = CyberCyan,
        summary = "Switches your default mobile data connection between physical SIM and eSIM in a fraction of a second without opening Android Settings menus.",
        whyNeeded = "Normally, switching mobile data lines requires navigating deep into Android Settings -> Network -> SIMs -> Data SIM -> Confirm. Autoroid bypasses this via direct binder calls.",
        steps = listOf(
            "Ensure you have two active lines (Physical SIM + eSIM or Dual Physical SIMs).",
            "The card displays which line is currently 'ACTIVE DATA' with a green indicator.",
            "Tap 'SWITCH DATA' (or tap either SIM card slot directly).",
            "Autoroid directly issues telephony IPC commands (ISub) to switch the active mobile data line.",
            "You can also use the SIM Switcher Quick Settings tile from your notification shade."
        ),
        tips = listOf(
            "Both Physical SIMs and eSIMs are fully supported.",
            "If connected to Wi-Fi, your phone will stay on Wi-Fi, but the cellular data default line switches immediately in the background."
        )
    ),
    WORKFLOWS(
        title = "Workflow Automations",
        subtitle = "Any-App Macros & Smart Click",
        icon = Icons.Default.AdsClick,
        accentColor = NeonGreen,
        summary = "Creates and executes multi-step macros across any installed Android application without requiring root-level scripting knowledge.",
        whyNeeded = "Automates repetitive daily routines like launching workout apps, killing frozen apps, or pressing sequences of buttons automatically.",
        steps = listOf(
            "Tap 'RUN' on any workflow card to start execution.",
            "Autoroid executes steps sequentially: launching apps, waiting for UI loading, clicking buttons, or injecting taps.",
            "Tap '+ New' to build your own custom workflow.",
            "Use 'Smart Click Text' to tap any button by its on-screen name, or 'Tap Coordinate' for exact pixel taps."
        ),
        tips = listOf(
            "If a workflow targets an app not installed on your phone (e.g. Samsung Health), it cannot launch.",
            "Tap 'Coords' to turn on the screen coordinate overlay to easily find exact tap positions."
        )
    ),
    COORDINATES(
        title = "Touch Coordinates (Pointer Location)",
        subtitle = "Pixel Position Overlay",
        icon = Icons.Default.TouchApp,
        accentColor = AmberWarn,
        summary = "Toggles Android's built-in Pointer Location developer overlay to show real-time screen touch coordinates.",
        whyNeeded = "When building tap macros, you need to know the exact (X, Y) pixel coordinates of buttons on your screen.",
        steps = listOf(
            "Tap 'Coords' in the Workflow Automations section.",
            "A technical status bar appears at the very top of your screen.",
            "Touch any spot on your screen: the bar displays exact coordinates (e.g. X: 540, Y: 1200).",
            "Use these coordinate numbers when creating 'Tap Coordinate' steps in workflows.",
            "Tap 'Coords ON' again to turn the overlay off."
        ),
        tips = listOf(
            "This overlay is rendered directly by the Android OS compositor with zero lag.",
            "Requires Root or Shizuku permission to toggle system developer settings."
        )
    )
}

@Composable
fun FeatureInfoDialog(
    helpType: FeatureHelpType,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            color = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(helpType.accentColor.copy(alpha = 0.15f))
                                .border(1.dp, helpType.accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = helpType.icon,
                                contentDescription = null,
                                tint = helpType.accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = helpType.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = helpType.subtitle,
                                fontSize = 11.sp,
                                color = helpType.accentColor
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary
                Text(
                    text = helpType.summary,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Why It's Needed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DeepBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "WHY THIS IS NEEDED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = helpType.whyNeeded,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step by Step
                Text(
                    text = "HOW TO USE IT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                helpType.steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = helpType.accentColor,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Troubleshooting Tips
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(helpType.accentColor.copy(alpha = 0.08f))
                        .border(1.dp, helpType.accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = helpType.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TROUBLESHOOTING & TIPS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = helpType.accentColor,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        helpType.tips.forEach { tip ->
                            Text(
                                text = "• $tip",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = helpType.accentColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
