package com.autoroid.app.feature.workflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.autoroid.app.feature.workflow.model.Workflow
import com.autoroid.app.feature.workflow.model.WorkflowStep
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DeepBackground
import com.autoroid.app.ui.theme.NeonRed
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary
import java.util.UUID

@Composable
fun WorkflowEditorDialog(
    initialWorkflow: Workflow? = null,
    onDismiss: () -> Unit,
    onSave: (Workflow) -> Unit
) {
    var name by remember { mutableStateOf(initialWorkflow?.name ?: "") }
    var description by remember { mutableStateOf(initialWorkflow?.description ?: "") }
    val steps = remember {
        mutableStateListOf<WorkflowStep>().apply {
            initialWorkflow?.steps?.let { addAll(it) }
        }
    }

    var showAppPicker by remember { mutableStateOf(false) }
    var showStepTypeMenu by remember { mutableStateOf(false) }

    // Dialog state for adding specific step
    var addingStepType by remember { mutableStateOf<String?>(null) }
    var inputParam1 by remember { mutableStateOf("") }
    var inputParam2 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Text(
                text = if (initialWorkflow == null) "Create Workflow" else "Edit Workflow",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workflow Name") },
                    placeholder = { Text("e.g. Start Workout") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = customTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Automatically launches Samsung Health") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = customTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Steps (${steps.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CyberCyan
                    )

                    Row {
                        OutlinedButton(
                            onClick = { showStepTypeMenu = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyberCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Step", color = CyberCyan, fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = showStepTypeMenu,
                            onDismissRequest = { showStepTypeMenu = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("1. Launch App", color = TextPrimary) },
                                onClick = {
                                    showStepTypeMenu = false
                                    showAppPicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("2. Smart Click (By Text)", color = TextPrimary) },
                                onClick = {
                                    showStepTypeMenu = false
                                    inputParam1 = ""
                                    addingStepType = "SMART_CLICK"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("3. Tap Coordinate (X, Y)", color = TextPrimary) },
                                onClick = {
                                    showStepTypeMenu = false
                                    inputParam1 = ""
                                    inputParam2 = ""
                                    addingStepType = "TAP_COORD"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("4. Wait Delay (ms)", color = TextPrimary) },
                                onClick = {
                                    showStepTypeMenu = false
                                    inputParam1 = "500"
                                    addingStepType = "DELAY"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("5. Raw Shell Command", color = TextPrimary) },
                                onClick = {
                                    showStepTypeMenu = false
                                    inputParam1 = ""
                                    addingStepType = "SHELL"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    itemsIndexed(steps) { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepBackground)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.width(22.dp)
                                )
                                Text(
                                    text = step.displaySummary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = { steps.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = NeonRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val workflow = Workflow(
                            id = initialWorkflow?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            description = description.trim(),
                            steps = steps.toList(),
                            createdAt = initialWorkflow?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(workflow)
                    }
                },
                enabled = name.isNotBlank() && steps.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Workflow", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    // Sub-dialog: App Picker
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { pkg, label ->
                steps.add(WorkflowStep.LaunchApp(packageName = pkg, appLabel = label))
                showAppPicker = false
            }
        )
    }

    // Sub-dialog: Input Prompts for steps
    if (addingStepType != null) {
        AlertDialog(
            onDismissRequest = { addingStepType = null },
            containerColor = CardSurface,
            title = {
                Text(
                    text = when (addingStepType) {
                        "SMART_CLICK" -> "Smart Click by Text"
                        "TAP_COORD" -> "Tap Coordinate"
                        "DELAY" -> "Wait Delay"
                        "SHELL" -> "Custom Shell Command"
                        else -> "Configure Step"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    when (addingStepType) {
                        "SMART_CLICK" -> {
                            Text("Enter button text to find on screen (e.g. \"Running\" or \"Start\"): ", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = inputParam1,
                                onValueChange = { inputParam1 = it },
                                placeholder = { Text("Button Text") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }
                        "TAP_COORD" -> {
                            Text("Screen Coordinates (use Pointer Location toggle if needed):", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                OutlinedTextField(
                                    value = inputParam1,
                                    onValueChange = { inputParam1 = it },
                                    label = { Text("X Coordinate") },
                                    modifier = Modifier.weight(1f),
                                    colors = customTextFieldColors()
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = inputParam2,
                                    onValueChange = { inputParam2 = it },
                                    label = { Text("Y Coordinate") },
                                    modifier = Modifier.weight(1f),
                                    colors = customTextFieldColors()
                                )
                            }
                        }
                        "DELAY" -> {
                            Text("Duration in milliseconds (e.g. 500 = 0.5s):", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = inputParam1,
                                onValueChange = { inputParam1 = it },
                                placeholder = { Text("500") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }
                        "SHELL" -> {
                            Text("Elevated shell command to execute:", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = inputParam1,
                                onValueChange = { inputParam1 = it },
                                placeholder = { Text("e.g. am force-stop com.example.app") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customTextFieldColors()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (addingStepType) {
                            "SMART_CLICK" -> {
                                if (inputParam1.isNotBlank()) {
                                    steps.add(WorkflowStep.SmartClickText(inputParam1.trim()))
                                }
                            }
                            "TAP_COORD" -> {
                                val x = inputParam1.trim().toIntOrNull() ?: 0
                                val y = inputParam2.trim().toIntOrNull() ?: 0
                                steps.add(WorkflowStep.TapCoordinate(x, y))
                            }
                            "DELAY" -> {
                                val ms = inputParam1.trim().toLongOrNull() ?: 500L
                                steps.add(WorkflowStep.Delay(ms))
                            }
                            "SHELL" -> {
                                if (inputParam1.isNotBlank()) {
                                    steps.add(WorkflowStep.ShellCommand(inputParam1.trim()))
                                }
                            }
                        }
                        addingStepType = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { addingStepType = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberCyan,
    unfocusedBorderColor = CardBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = CyberCyan,
    unfocusedLabelColor = TextSecondary
)
