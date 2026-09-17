package com.autoroid.app.feature.update.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.autoroid.app.feature.update.model.UpdateInfo
import com.autoroid.app.feature.update.model.UpdateState
import com.autoroid.app.feature.update.model.UpdateStatus
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DeepBackground
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.NeonRed
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary

@Composable
fun UpdateStatusDialog(
    currentVersion: String,
    updateStatus: UpdateStatus,
    onCheckAgain: () -> Unit,
    onDownloadAndInstall: (UpdateInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val state = updateStatus.state
    val info = updateStatus.updateInfo
    val isBusy = state == UpdateState.CHECKING || state == UpdateState.DOWNLOADING || state == UpdateState.INSTALLING

    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
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
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Software Update",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "GitHub Releases System",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (!isBusy) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Version Comparison Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Current Version Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepBackground)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "INSTALLED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "v$currentVersion",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }

                    // Latest Release Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepBackground)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "LATEST ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val latestText = info?.latestVersion?.let {
                                if (it.startsWith("v", ignoreCase = true)) it else "v$it"
                            } ?: if (state == UpdateState.CHECKING) "Checking..." else "v$currentVersion"
                            
                            val isUpdateAvailable = state == UpdateState.AVAILABLE
                            Text(
                                text = latestText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isUpdateAvailable) NeonGreen else CyberCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Message Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (state) {
                                UpdateState.AVAILABLE -> NeonGreen.copy(alpha = 0.1f)
                                UpdateState.UP_TO_DATE -> CyberCyan.copy(alpha = 0.1f)
                                UpdateState.ERROR -> NeonRed.copy(alpha = 0.1f)
                                else -> DeepBackground
                            }
                        )
                        .border(
                            1.dp,
                            when (state) {
                                UpdateState.AVAILABLE -> NeonGreen.copy(alpha = 0.3f)
                                UpdateState.UP_TO_DATE -> CyberCyan.copy(alpha = 0.3f)
                                UpdateState.ERROR -> NeonRed.copy(alpha = 0.3f)
                                else -> CardBorder
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (state) {
                            UpdateState.CHECKING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Querying GitHub Releases API...",
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                            UpdateState.UP_TO_DATE -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Autoroid is up to date! You are running the newest release.",
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                            UpdateState.AVAILABLE -> {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (info?.isDownloaded == true) {
                                        "Release ${info.latestVersion} is downloaded and ready to install!"
                                    } else {
                                        "New release ${info?.latestVersion} is available to install!"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeonGreen
                                )
                            }
                            UpdateState.DOWNLOADING -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Downloading APK package...",
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${updateStatus.progressPercent}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { updateStatus.progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = CyberCyan,
                                        trackColor = DeepBackground
                                    )
                                }
                            }
                            UpdateState.INSTALLING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonGreen
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Elevated self-install in progress...",
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                            UpdateState.ERROR -> {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = NeonRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = updateStatus.message.ifBlank { "Could not connect to GitHub." },
                                    fontSize = 13.sp,
                                    color = NeonRed
                                )
                            }
                            UpdateState.IDLE -> {
                                Text(
                                    text = updateStatus.message.ifBlank { "Ready to check for updates." },
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Release Notes (if present)
                if (info != null && info.releaseNotes.isNotBlank() && info.releaseNotes != "No release notes provided.") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "RELEASE NOTES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepBackground)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = info.releaseNotes.trim(),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state == UpdateState.AVAILABLE && info != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Later", color = TextSecondary)
                        }
                        Button(
                            onClick = { onDownloadAndInstall(info) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                        ) {
                            Text(
                                text = if (info.isDownloaded) "Install Update" else "Download & Install",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (state == UpdateState.DOWNLOADING || state == UpdateState.INSTALLING) {
                        Button(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (state == UpdateState.DOWNLOADING) "Downloading..." else "Installing...")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onCheckAgain,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isBusy
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Check Again", color = CyberCyan, fontSize = 13.sp)
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            enabled = !isBusy
                        ) {
                            Text(text = "Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
