package com.autoroid.app.ui.screens.tabs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoroid.app.feature.workflow.model.Workflow
import com.autoroid.app.feature.workflow.runner.ExecutionState
import com.autoroid.app.feature.workflow.ui.WorkflowCard
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DarkSurface
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.TextMuted
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary

@Composable
fun WorkflowsScreenView(
    workflows: List<Workflow>,
    executionState: ExecutionState,
    isPointerLocationActive: Boolean,
    isPackageInstalled: (String) -> Boolean,
    onRunWorkflow: (Workflow) -> Unit,
    onEditWorkflow: (Workflow) -> Unit,
    onDeleteWorkflow: (String) -> Unit,
    onNewWorkflowClick: () -> Unit,
    onTogglePointerLocation: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Quick Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MACRO AUTOMATIONS",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CyberCyan,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = onHelpClick, modifier = Modifier.size(20.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "Workflows Guide",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${workflows.size} active automation pipeline(s)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = onTogglePointerLocation,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isPointerLocationActive) NeonGreen.copy(alpha = 0.5f) else CardBorder),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPointerLocationActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isPointerLocationActive) NeonGreen else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPointerLocationActive) "Coords ON" else "Coords",
                                    fontSize = 11.sp,
                                    color = if (isPointerLocationActive) NeonGreen else TextSecondary
                                )
                            }

                            Button(
                                onClick = onNewWorkflowClick,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Empty state
        if (workflows.isEmpty()) {
            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Workflows Created",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Automate multi-step tasks in any app with smart clicks, coordinate taps, and elevated gestures.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Workflows Feed
        items(workflows) { workflow ->
            WorkflowCard(
                workflow = workflow,
                executionState = executionState,
                isPackageInstalled = isPackageInstalled,
                onRun = { onRunWorkflow(workflow) },
                onEdit = { onEditWorkflow(workflow) },
                onDelete = { onDeleteWorkflow(workflow.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
