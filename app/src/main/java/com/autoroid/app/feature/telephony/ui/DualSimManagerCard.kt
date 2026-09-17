package com.autoroid.app.feature.telephony.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsCell
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SyncAlt
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
import androidx.compose.ui.unit.sp
import com.autoroid.app.feature.telephony.SimSlotInfo
import com.autoroid.app.feature.telephony.schedule.model.SimSchedule
import com.autoroid.app.ui.dialogs.TimePickerDialog
import com.autoroid.app.ui.theme.*
import java.util.Calendar

enum class ModemMode {
    MANUAL,
    SCHEDULE
}

@Composable
fun DualSimManagerCard(
    simSlots: List<SimSlotInfo>,
    activeSubId: Int?,
    schedule: SimSchedule,
    onToggleAlternateSim: () -> Unit,
    onSelectSubId: (Int) -> Unit,
    onUpdateSchedule: (SimSchedule) -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onHelpClick: () -> Unit = {}
) {
    var activeMode by remember { mutableStateOf(ModemMode.MANUAL) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Pulsing animation for active data line
    val infiniteTransition = rememberInfiniteTransition(label = "ActiveSimPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Resolved alternate SIM
    val alternateSim = simSlots.firstOrNull { it.subscriptionId != activeSubId && !it.isDefaultData }
        ?: simSlots.firstOrNull { it.subscriptionId != activeSubId }

    // Auto-resolve reasonable defaults for schedule if not configured yet
    val resolvedWindowSubId = remember(schedule.windowSubId, simSlots) {
        if (schedule.windowSubId > 0 && simSlots.any { it.subscriptionId == schedule.windowSubId }) {
            schedule.windowSubId
        } else {
            simSlots.firstOrNull { it.isEmbedded }?.subscriptionId
                ?: simSlots.getOrNull(1)?.subscriptionId
                ?: simSlots.firstOrNull()?.subscriptionId
                ?: -1
        }
    }

    val resolvedDefaultSubId = remember(schedule.defaultSubId, simSlots, resolvedWindowSubId) {
        if (schedule.defaultSubId > 0 && simSlots.any { it.subscriptionId == schedule.defaultSubId }) {
            schedule.defaultSubId
        } else {
            simSlots.firstOrNull { it.subscriptionId != resolvedWindowSubId }?.subscriptionId
                ?: simSlots.firstOrNull()?.subscriptionId
                ?: -1
        }
    }

    // Auto-populate default IDs if unset
    LaunchedEffect(resolvedWindowSubId, resolvedDefaultSubId) {
        if ((schedule.windowSubId <= 0 && resolvedWindowSubId > 0) ||
            (schedule.defaultSubId <= 0 && resolvedDefaultSubId > 0)
        ) {
            onUpdateSchedule(
                schedule.copy(
                    windowSubId = if (schedule.windowSubId <= 0) resolvedWindowSubId else schedule.windowSubId,
                    defaultSubId = if (schedule.defaultSubId <= 0) resolvedDefaultSubId else schedule.defaultSubId
                )
            )
        }
    }

    val currentCalendar = Calendar.getInstance()
    val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCalendar.get(Calendar.MINUTE)
    val isInWindowNow = schedule.isTimeInWindow(currentHour, currentMinute)

    val windowSim = simSlots.firstOrNull { it.subscriptionId == resolvedWindowSubId }
    val defaultSim = simSlots.firstOrNull { it.subscriptionId == resolvedDefaultSubId }

    // Futuristic Gradient Border Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberCyan.copy(alpha = 0.5f),
                    CardBorder,
                    NeonGreen.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Title, Subtitle, Help Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = CyberCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SettingsCell,
                                contentDescription = "Modem Manager",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dual-SIM & Modem Manager",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val activeSimInfo = simSlots.firstOrNull { it.subscriptionId == activeSubId || it.isDefaultData }
                            val activeLabel = activeSimInfo?.let { "${it.simType} (${it.displayLabel})" } ?: "Active Data: Auto"
                            Text(
                                text = "$activeLabel • ${simSlots.size} Detected",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                IconButton(onClick = onHelpClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Modem Help",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hardware SIM Slots Grid (Interactive microchip cards)
            if (simSlots.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    simSlots.forEach { slot ->
                        val isCurrentData = (slot.subscriptionId == activeSubId) || slot.isDefaultData
                        val simColor = if (slot.isEmbedded) CyberCyan else NeonGreen

                        Surface(
                            onClick = { onSelectSubId(slot.subscriptionId) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrentData) DeepBackground else DarkSurface,
                            border = BorderStroke(
                                width = if (isCurrentData) 1.5.dp else 1.dp,
                                color = if (isCurrentData) simColor.copy(alpha = pulseAlpha) else CardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Hardware Chip Icon / Type Badge
                                    Surface(
                                        color = if (isCurrentData) simColor.copy(alpha = 0.2f) else CardSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isCurrentData) simColor.copy(alpha = 0.5f) else CardBorder),
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SimCard,
                                                contentDescription = null,
                                                tint = if (isCurrentData) simColor else TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (slot.isEmbedded) "eSIM" else "SIM ${if (slot.slotIndex >= 0) slot.slotIndex + 1 else slot.subscriptionId}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentData) simColor else TextPrimary
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = slot.displayLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${slot.carrierName} • SubId ${slot.subscriptionId}",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (isCurrentData) {
                                    Surface(
                                        color = NeonGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(NeonGreen, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "ACTIVE DATA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "TAP TO SWITCH",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CyberCyan
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = AmberWarn.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AmberWarn,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No SIMs detected yet • Grant Phone permission or connect Shizuku.",
                            fontSize = 11.sp,
                            color = AmberWarn,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: Capsule Pill Switcher (Manual vs Auto Schedule)
            Surface(
                color = DeepBackground,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Manual Mode Tab
                    Surface(
                        onClick = { activeMode = ModemMode.MANUAL },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeMode == ModemMode.MANUAL) CyberCyan.copy(alpha = 0.18f) else Color.Transparent,
                        border = if (activeMode == ModemMode.MANUAL) BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = if (activeMode == ModemMode.MANUAL) CyberCyan else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Instant Switch",
                                fontSize = 12.sp,
                                fontWeight = if (activeMode == ModemMode.MANUAL) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeMode == ModemMode.MANUAL) CyberCyan else TextSecondary
                            )
                        }
                    }

                    // Auto Schedule Tab
                    Surface(
                        onClick = { activeMode = ModemMode.SCHEDULE },
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeMode == ModemMode.SCHEDULE) NeonGreen.copy(alpha = 0.18f) else Color.Transparent,
                        border = if (activeMode == ModemMode.SCHEDULE) BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (activeMode == ModemMode.SCHEDULE) NeonGreen else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto Schedule",
                                fontSize = 12.sp,
                                fontWeight = if (activeMode == ModemMode.SCHEDULE) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeMode == ModemMode.SCHEDULE) NeonGreen else TextSecondary
                            )
                            if (schedule.isEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Content: Manual Switch Mode
            AnimatedVisibility(
                visible = activeMode == ModemMode.MANUAL,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Button(
                        onClick = onToggleAlternateSim,
                        enabled = simSlots.size >= 2,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val buttonText = when {
                            simSlots.size >= 2 && alternateSim != null ->
                                "SWITCH DATA TO ${alternateSim.simType.uppercase()} (${alternateSim.displayLabel.uppercase()})"
                            simSlots.size >= 2 -> "TOGGLE ALTERNATE SIM"
                            else -> "DUAL-SIM REQUIRED (${simSlots.size} DETECTED)"
                        }
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Instantly commands direct telephony binder IPC (ISub) without going through Android Settings menus.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // Mode Content: Auto Schedule Mode
            AnimatedVisibility(
                visible = activeMode == ModemMode.SCHEDULE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    // Schedule Master Toggle Row
                    Surface(
                        color = DeepBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (schedule.isEnabled) "Automated Schedule Active" else "Schedule Paused",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (schedule.isEnabled) NeonGreen else TextSecondary
                                )
                                Text(
                                    text = if (schedule.isEnabled)
                                        "Automated switching active in background (Survives Doze & Boot)"
                                    else
                                        "Enable to automate data switches by time window",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Switch(
                                checked = schedule.isEnabled,
                                onCheckedChange = { onToggleSchedule(it) },
                                enabled = simSlots.size >= 2,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonGreen,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = CardSurface
                                )
                            )
                        }
                    }

                    // Live Status Banner
                    if (schedule.isEnabled && simSlots.size >= 2) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val bannerColor = if (isInWindowNow) CyberCyan else NeonGreen
                        Surface(
                            color = bannerColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(bannerColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isInWindowNow) "OFF-PEAK WINDOW RUNNING" else "DAYTIME LINE ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = bannerColor,
                                        letterSpacing = 1.sp
                                    )
                                    val statusDetail = if (isInWindowNow) {
                                        "Routed to ${windowSim?.let { "${it.simType} (${it.displayLabel})" } ?: "Window SIM"} until ${schedule.formattedEndTime}"
                                    } else {
                                        "Routed to ${defaultSim?.let { "${it.simType} (${it.displayLabel})" } ?: "Default SIM"} • Switches at ${schedule.formattedStartTime}"
                                    }
                                    Text(
                                        text = statusDetail,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Time Window Pickers (Start -> End)
                    Text(
                        text = "1. SCHEDULE TIME WINDOW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Window Start
                        Surface(
                            onClick = { showStartTimePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = DeepBackground,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "FROM (START)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = schedule.formattedStartTime,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap to change",
                                    fontSize = 10.sp,
                                    color = CyberCyan
                                )
                            }
                        }

                        // Window End
                        Surface(
                            onClick = { showEndTimePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = DeepBackground,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TO (END)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = schedule.formattedEndTime,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tap to change",
                                    fontSize = 10.sp,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    // SIM Assignments
                    if (simSlots.size >= 2) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "2. SIM ASSIGNMENTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Inside Window assignment
                        Text(
                            text = "During Window (${schedule.formattedStartTime} – ${schedule.formattedEndTime}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            simSlots.forEach { slot ->
                                val isSelected = (slot.subscriptionId == resolvedWindowSubId)
                                Surface(
                                    onClick = {
                                        onUpdateSchedule(schedule.copy(windowSubId = slot.subscriptionId))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else DeepBackground,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) CyberCyan else CardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = slot.simType,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CyberCyan else TextSecondary
                                            )
                                            Text(
                                                text = slot.displayLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Outside Window assignment
                        Text(
                            text = "For All Other Hours (Daytime / Primary Line):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            simSlots.forEach { slot ->
                                val isSelected = (slot.subscriptionId == resolvedDefaultSubId)
                                Surface(
                                    onClick = {
                                        onUpdateSchedule(schedule.copy(defaultSubId = slot.subscriptionId))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NeonGreen.copy(alpha = 0.15f) else DeepBackground,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) NeonGreen else CardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = slot.simType,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) NeonGreen else TextSecondary
                                            )
                                            Text(
                                                text = slot.displayLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = NeonGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Time Pickers Dialogs
    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Window Start Time",
            initialHour = schedule.startHour,
            initialMinute = schedule.startMinute,
            onConfirm = { h, m ->
                onUpdateSchedule(schedule.copy(startHour = h, startMinute = m))
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Window End Time",
            initialHour = schedule.endHour,
            initialMinute = schedule.endMinute,
            onConfirm = { h, m ->
                onUpdateSchedule(schedule.copy(endHour = h, endMinute = m))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}
