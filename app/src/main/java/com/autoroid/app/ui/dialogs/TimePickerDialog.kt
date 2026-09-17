package com.autoroid.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.autoroid.app.ui.theme.*
import java.util.Locale

@Composable
fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour24 by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    // Convert to 12-hour format for UI controls
    var isPm by remember { mutableStateOf(initialHour >= 12) }
    var hour12 by remember {
        mutableIntStateOf(
            when {
                initialHour == 0 -> 12
                initialHour > 12 -> initialHour - 12
                else -> initialHour
            }
        )
    }

    fun sync24Hour() {
        selectedHour24 = when {
            isPm && hour12 < 12 -> hour12 + 12
            !isPm && hour12 == 12 -> 0
            isPm && hour12 == 12 -> 12
            else -> hour12
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardSurface,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big digital clock preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DeepBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, selectedMinute, if (isPm) "PM" else "AM"),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberCyan,
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .padding(vertical = 14.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hour & Minute Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "HOUR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    hour12 = if (hour12 <= 1) 12 else hour12 - 1
                                    sync24Hour()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Hour", tint = CyberCyan)
                            }

                            Text(
                                text = String.format(Locale.getDefault(), "%02d", hour12),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = {
                                    hour12 = if (hour12 >= 12) 1 else hour12 + 1
                                    sync24Hour()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Hour", tint = CyberCyan)
                            }
                        }
                    }

                    // Minute selector (steps of 5 or 1)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "MINUTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    selectedMinute = if (selectedMinute <= 0) 55 else (selectedMinute - 5).coerceAtLeast(0)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Minute", tint = CyberCyan)
                            }

                            Text(
                                text = String.format(Locale.getDefault(), "%02d", selectedMinute),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = {
                                    selectedMinute = if (selectedMinute >= 55) 0 else (selectedMinute + 5).coerceAtMost(55)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Minute", tint = CyberCyan)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AM / PM Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = !isPm,
                        onClick = {
                            isPm = false
                            sync24Hour()
                        },
                        label = { Text("AM", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    FilterChip(
                        selected = isPm,
                        onClick = {
                            isPm = true
                            sync24Hour()
                        },
                        label = { Text("PM", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            sync24Hour()
                            onConfirm(selectedHour24, selectedMinute)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SET TIME", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
