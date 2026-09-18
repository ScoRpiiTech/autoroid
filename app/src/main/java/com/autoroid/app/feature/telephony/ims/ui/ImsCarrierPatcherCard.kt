package com.autoroid.app.feature.telephony.ims.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsCell
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.feature.telephony.SimSlotInfo
import com.autoroid.app.feature.telephony.ims.model.ImsConfig
import com.autoroid.app.ui.theme.*

@Composable
fun ImsCarrierPatcherCard(
    simSlots: List<SimSlotInfo>,
    imsConfigs: Map<Int, ImsConfig>,
    isApplying: Boolean,
    privilegeLevel: PrivilegeLevel = PrivilegeLevel.NONE,
    lastResult: String? = null,
    onRequestShizuku: () -> Unit = {},
    onSaveConfig: (ImsConfig) -> Unit,
    onApplyConfig: (slotIndex: Int, subId: Int, ImsConfig) -> Unit,
    onApplyAll: () -> Unit,
    onResetConfig: (slotIndex: Int, subId: Int) -> Unit,
    onHelpClick: () -> Unit = {}
) {
    // Current selected SIM slot tab (defaults to first available or 0)
    var selectedSlotIndex by remember { mutableStateOf(0) }

    // Ensure selectedSlotIndex is valid if slots change
    val activeSlot = simSlots.firstOrNull { it.slotIndex == selectedSlotIndex }
        ?: simSlots.firstOrNull()

    val currentSlotIndex = activeSlot?.slotIndex ?: selectedSlotIndex
    val currentSubId = activeSlot?.subscriptionId ?: (currentSlotIndex + 1)

    // Current config for this slot
    val currentConfig = imsConfigs[currentSlotIndex] ?: remember(currentSlotIndex, currentSubId) {
        ImsConfig(slotIndex = currentSlotIndex, subscriptionId = currentSubId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.6f), CardBorder, NeonGreen.copy(alpha = 0.5f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsCell,
                            contentDescription = "IMS Patcher",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CARRIER & IMS PATCHER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Pixel VoLTE, VoWiFi & 5G VoNR Enabler",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "IMS Patcher Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shizuku Privilege Check Banner
            if (privilegeLevel == PrivilegeLevel.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberWarn.copy(alpha = 0.12f))
                        .border(1.dp, AmberWarn.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarn, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shizuku or Root required for Pixel IMS patching.",
                                fontSize = 11.sp,
                                color = AmberWarn,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Button(
                            onClick = onRequestShizuku,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // SIM Slot Selector Tabs
            if (simSlots.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F141C))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (slot in simSlots) {
                        val isSelected = (slot.slotIndex == currentSlotIndex)
                        val tabBg = if (isSelected) CyberCyan.copy(alpha = 0.18f) else Color.Transparent
                        val tabBorder = if (isSelected) CyberCyan else Color.Transparent
                        val textCol = if (isSelected) CyberCyan else TextSecondary

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tabBg)
                                .clickable { selectedSlotIndex = slot.slotIndex }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = null,
                                    tint = textCol,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${slot.displayLabel} (${slot.simType})",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = textCol,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Live Override Status Banner
            val isApplied = currentConfig.isApplied
            val statusBg = if (isApplied) NeonGreen.copy(alpha = 0.12f) else AmberWarn.copy(alpha = 0.10f)
            val statusBorder = if (isApplied) NeonGreen.copy(alpha = 0.4f) else AmberWarn.copy(alpha = 0.35f)
            val statusColor = if (isApplied) NeonGreen else AmberWarn

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isApplied) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isApplied) "OVERRIDES ACTIVE & SAVED" else "FACTORY CARRIER DEFAULTS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = statusColor
                            )
                            Text(
                                text = if (isApplied) {
                                    "Auto-restores across phone reboots via BootReceiver"
                                } else {
                                    "Tap 'Apply IMS Overrides' to bypass carrier lock"
                                },
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (isApplied) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PROTECTED",
                                color = NeonGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feature Toggles Section
            Text(
                text = "CARRIER CONFIGURATION FLAGS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. VoLTE Toggle
            ImsToggleRow(
                title = "VoLTE (Voice over 4G LTE)",
                subtitle = "Force-enables HD Voice calling on unsupported 4G networks",
                checked = currentConfig.volteEnabled,
                onCheckedChange = { checked ->
                    val updated = currentConfig.copy(volteEnabled = checked)
                    onSaveConfig(updated)
                }
            )

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            // 2. VoWiFi Toggle
            ImsToggleRow(
                title = "Wi-Fi Calling (VoWiFi / WFC)",
                subtitle = "Enables cellular voice calls through local Wi-Fi networks",
                checked = currentConfig.vowifiEnabled,
                onCheckedChange = { checked ->
                    val updated = currentConfig.copy(vowifiEnabled = checked)
                    onSaveConfig(updated)
                }
            )

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            // 3. 5G VoNR Toggle
            ImsToggleRow(
                title = "5G Voice Calling (VoNR)",
                subtitle = "Keeps voice calls on pure 5G SA without EPS fallback to 4G",
                checked = currentConfig.vonrEnabled,
                onCheckedChange = { checked ->
                    val updated = currentConfig.copy(vonrEnabled = checked)
                    onSaveConfig(updated)
                }
            )

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            // 4. Ut Interface Toggle
            ImsToggleRow(
                title = "Supplementary Services (Ut Interface)",
                subtitle = "Enables Call Forwarding, Call Waiting, and USSD over IMS",
                checked = currentConfig.utInterfaceEnabled,
                onCheckedChange = { checked ->
                    val updated = currentConfig.copy(utInterfaceEnabled = checked)
                    onSaveConfig(updated)
                }
            )

            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            // 5. Settings Visibility Toggle
            ImsToggleRow(
                title = "Android Settings Toggle Visibility",
                subtitle = "Makes VoLTE and Wi-Fi Calling switches visible in System Settings",
                checked = currentConfig.settingsVisibilityEnabled,
                onCheckedChange = { checked ->
                    val updated = currentConfig.copy(settingsVisibilityEnabled = checked)
                    onSaveConfig(updated)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action: Apply to Current SIM
                Button(
                    onClick = { onApplyConfig(currentSlotIndex, currentSubId, currentConfig) },
                    enabled = !isApplying,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APPLYING OVERRIDES...",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APPLY TO SIM ${currentSlotIndex + 1} (${activeSlot?.displayLabel ?: "ACTIVE"})",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                // Secondary Row: Apply to All SIMs + Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Apply to All Active SIMs
                    OutlinedButton(
                        onClick = onApplyAll,
                        enabled = !isApplying,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen)
                    ) {
                        Text(
                            text = "⚡ APPLY TO ALL SIMS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }

                    // Reset to Default
                    OutlinedButton(
                        onClick = { onResetConfig(currentSlotIndex, currentSubId) },
                        enabled = !isApplying,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RESET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Live Operation Result Banner
                if (!lastResult.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isSuccess = !lastResult.contains("Failed", ignoreCase = true) &&
                                    !lastResult.contains("Error", ignoreCase = true) &&
                                    !lastResult.contains("not granted", ignoreCase = true) &&
                                    !lastResult.contains("not running", ignoreCase = true)
                    val resultBg = if (isSuccess) NeonGreen.copy(alpha = 0.10f) else AmberWarn.copy(alpha = 0.12f)
                    val resultBorder = if (isSuccess) NeonGreen.copy(alpha = 0.35f) else AmberWarn.copy(alpha = 0.4f)
                    val resultColor = if (isSuccess) NeonGreen else AmberWarn

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(resultBg)
                            .border(1.dp, resultBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = null,
                                tint = resultColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lastResult,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = resultColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF1B2330)
            )
        )
    }
}
