package com.autoroid.app.feature.update.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoroid.app.feature.update.model.UpdateInfo
import com.autoroid.app.feature.update.model.UpdateState
import com.autoroid.app.feature.update.model.UpdateStatus
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DeepBackground
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary

@Composable
fun UpdateBannerCard(
    updateStatus: UpdateStatus,
    onDownloadAndInstall: (UpdateInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val info = updateStatus.updateInfo ?: return
    val isDownloading = updateStatus.state == UpdateState.DOWNLOADING
    val isInstalling = updateStatus.state == UpdateState.INSTALLING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NeonGreen)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Update Available: ${info.latestVersion}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (info.isDownloaded) "Downloaded • Ready to install" else "GitHub Release • Ready to download",
                            fontSize = 11.sp,
                            color = NeonGreen
                        )
                    }
                }

                if (!isDownloading && !isInstalling) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (info.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "What's New:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.releaseNotes.trim(),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        maxLines = 4
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { updateStatus.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonGreen,
                    trackColor = DeepBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = updateStatus.message,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
            } else if (isInstalling) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Installing via elevated package manager...",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonGreen
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onDownloadAndInstall(info) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (info.isDownloaded) "SELF-INSTALL UPDATE" else "DOWNLOAD & SELF-INSTALL",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
