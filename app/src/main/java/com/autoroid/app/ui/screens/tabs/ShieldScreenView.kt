package com.autoroid.app.ui.screens.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoroid.app.ui.theme.AmberWarn
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DarkSurface
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.NeonRed
import com.autoroid.app.ui.theme.TextMuted
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary

@Composable
fun ShieldScreenView(
    isActive: Boolean,
    activeServices: List<String>,
    pausedServices: List<String>,
    onToggle: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllClear = !isActive && activeServices.isEmpty()

    val statusColor by animateColorAsState(
        targetValue = when {
            isActive -> NeonGreen
            isAllClear -> CyberCyan
            else -> AmberWarn
        },
        label = "statusColor"
    )

    val statusTitle = when {
        isActive -> "SHIELD ENGAGED"
        isAllClear -> "ALL CLEAR"
        else -> "VULNERABLE"
    }

    val statusSubtitle = when {
        isActive -> "Banking protection is active. ${pausedServices.size} service(s) paused."
        isAllClear -> "0 running accessibility services detected. Safe for all banking apps."
        else -> "${activeServices.size} active accessibility service(s) exposed to banking apps."
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Shield Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BANK MODE SHIELD",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(onClick = onHelpClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Shield Guide",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Glowing Halo Ring
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.10f))
                            .border(2.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.18f))
                                .border(1.dp, statusColor.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isActive) Icons.Default.Lock else if (isAllClear) Icons.Default.CheckCircle else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = statusTitle,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = statusSubtitle,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Big tactile toggle button
                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) NeonRed else if (isAllClear) DarkSurface else NeonGreen,
                            contentColor = if (isActive || isAllClear) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isActive -> "Restore Accessibility Services"
                                    isAllClear -> "Enforce Clean State"
                                    else -> "Engage Bank Shield Now"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Active / Paused Services Inspection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SYSTEM REGISTRY INSPECTION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeServices.isEmpty() && pausedServices.isEmpty()) {
                        Surface(
                            color = DarkSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Clean system: No third-party accessibility services installed or running.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    if (activeServices.isNotEmpty()) {
                        Text(
                            text = "Detected Running Services (${activeServices.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarn
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        activeServices.forEach { service ->
                            val simpleName = service.substringAfterLast("/").removePrefix(".")
                            Surface(
                                color = DarkSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, AmberWarn.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AmberWarn)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = simpleName,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (pausedServices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Preserved Services (${pausedServices.size}) • Ready for 1-tap restore:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        pausedServices.forEach { service ->
                            val simpleName = service.substringAfterLast("/").removePrefix(".")
                            Surface(
                                color = DarkSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = simpleName,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Settings Tile Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Quick Settings Shade Tile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add the 'Bank Shield' tile to your Android notification shade for 1-tap toggling before opening banking apps.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
