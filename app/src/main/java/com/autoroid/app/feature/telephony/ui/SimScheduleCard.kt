package com.autoroid.app.feature.telephony.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoroid.app.feature.telephony.SimSlotInfo
import com.autoroid.app.feature.telephony.schedule.model.SimSchedule
import com.autoroid.app.ui.dialogs.TimePickerDialog
import com.autoroid.app.ui.theme.*
import java.util.Calendar

@Composable
fun SimScheduleCard(
    schedule: SimSchedule,
    simSlots: List<SimSlotInfo>,
    activeSubId: Int?,
    onUpdateSchedule: (SimSchedule) -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onHelpClick: () -> Unit = {}
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Auto-resolve reasonable defaults if sub IDs are not set yet
    val resolvedWindowSubId = remember(schedule.windowSubId, simSlots) {
        if (schedule.windowSubId > 0 && simSlots.any { it.subscriptionId == schedule.windowSubId }) {
            schedule.windowSubId
        } else {
            // Default window SIM to eSIM if present, otherwise second SIM
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
            // Default line to Physical SIM if present, or first SIM different from window
            simSlots.firstOrNull { it.subscriptionId != resolvedWindowSubId }?.subscriptionId
                ?: simSlots.firstOrNull()?.subscriptionId
                ?: -1
        }
    }

    // Auto-populate default IDs if they were unset (-1)
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

    // Calculate current live status
    val currentCalendar = Calendar.getInstance()
    val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCalendar.get(Calendar.MINUTE)
    val isInWindowNow = schedule.isTimeInWindow(currentHour, currentMinute)

    val windowSim = simSlots.firstOrNull { it.subscriptionId == resolvedWindowSubId }
    val defaultSim = simSlots.firstOrNull { it.subscriptionId == resolvedDefaultSubId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "SIM Schedule",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Auto SIM Scheduler",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Time-windowed data bundles & off-peak switching",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onHelpClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "SIM Schedule Help",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Master Toggle Row
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
                            text = if (schedule.isEnabled) "Schedule Active" else "Schedule Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (schedule.isEnabled) NeonGreen else TextSecondary
                        )
                        Text(
                            text = if (schedule.isEnabled)
                                "Automated switching running in background"
                            else
                                "Enable to automate SIM data switches by time",
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

            // Dual-SIM Check warning
            if (simSlots.size < 2) {
                Spacer(modifier = Modifier.height(10.dp))
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
                            text = "Dual-SIM required (Physical SIM + eSIM or 2 SIM cards) to use automatic scheduling.",
                            fontSize = 11.sp,
                            color = AmberWarn,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Active Status Live Banner (when schedule is enabled)
            if (schedule.isEnabled && simSlots.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
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
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Surface(color = bannerColor, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxSize()) {}
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isInWindowNow) "OFF-PEAK WINDOW ACTIVE" else "DAYTIME / DEFAULT LINE ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
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

            // Configuration Section
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "1. SCHEDULE WINDOW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Time Window Buttons (Start Time -> End Time)
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
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "FROM (START)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = schedule.formattedStartTime,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to edit",
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
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "TO (END)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = schedule.formattedEndTime,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to edit",
                            fontSize = 10.sp,
                            color = NeonGreen
                        )
                    }
                }
            }

            // SIM Assignment Section
            if (simSlots.size >= 2) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "2. SIM ASSIGNMENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Assignment 1: Inside Window
                Text(
                    text = "During Window (${schedule.formattedStartTime} – ${schedule.formattedEndTime}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slot.simType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CyberCyan else TextSecondary
                                    )
                                    Text(
                                        text = slot.displayLabel,
                                        fontSize = 12.sp,
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Assignment 2: Outside Window (Default Line)
                Text(
                    text = "For All Other Hours (Daytime / Primary Line):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slot.simType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonGreen else TextSecondary
                                    )
                                    Text(
                                        text = slot.displayLabel,
                                        fontSize = 12.sp,
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
                                        modifier = Modifier.size(16.dp)
                                    )
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
