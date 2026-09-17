package com.autoroid.app.feature.workflow.runner

import android.content.Context
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.workflow.model.Workflow
import com.autoroid.app.feature.workflow.model.WorkflowStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

data class ExecutionState(
    val workflowId: String? = null,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val statusMessage: String = "Idle",
    val isRunning: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class WorkflowRunner(
    private val context: Context,
    private val privilegeManager: PrivilegeManager
) {
    private val _executionState = MutableStateFlow(ExecutionState())
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    suspend fun executeWorkflow(workflow: Workflow): Boolean = withContext(Dispatchers.IO) {
        val total = workflow.steps.size
        _executionState.value = ExecutionState(
            workflowId = workflow.id,
            currentStepIndex = 0,
            totalSteps = total,
            statusMessage = "Starting \"${workflow.name}\"...",
            isRunning = true
        )

        for ((index, step) in workflow.steps.withIndex()) {
            _executionState.value = ExecutionState(
                workflowId = workflow.id,
                currentStepIndex = index + 1,
                totalSteps = total,
                statusMessage = "Step ${index + 1}/$total: ${step.displaySummary}",
                isRunning = true
            )

            val success = executeStep(step)
            if (!success) {
                _executionState.value = ExecutionState(
                    workflowId = workflow.id,
                    currentStepIndex = index + 1,
                    totalSteps = total,
                    statusMessage = "Failed at Step ${index + 1}",
                    isRunning = false,
                    isSuccess = false,
                    error = "Failed: ${step.displaySummary}"
                )
                return@withContext false
            }
        }

        _executionState.value = ExecutionState(
            workflowId = workflow.id,
            currentStepIndex = total,
            totalSteps = total,
            statusMessage = "Completed \"${workflow.name}\" successfully!",
            isRunning = false,
            isSuccess = true
        )
        true
    }

    private suspend fun executeStep(step: WorkflowStep): Boolean {
        return when (step) {
            is WorkflowStep.LaunchApp -> launchApp(step.packageName)
            is WorkflowStep.TapCoordinate -> {
                val res = privilegeManager.executeElevated("input tap ${step.x} ${step.y}")
                res.isSuccess
            }
            is WorkflowStep.SmartClickText -> smartClickText(step.targetText)
            is WorkflowStep.Delay -> {
                delay(step.durationMs)
                true
            }
            is WorkflowStep.Swipe -> {
                val res = privilegeManager.executeElevated(
                    "input swipe ${step.startX} ${step.startY} ${step.endX} ${step.endY} ${step.durationMs}"
                )
                res.isSuccess
            }
            is WorkflowStep.ShellCommand -> {
                val res = privilegeManager.executeElevated(step.command)
                res.isSuccess
            }
        }
    }

    private suspend fun launchApp(packageName: String): Boolean {
        // Attempt clean launch using package manager's primary intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        val cmd = if (launchIntent?.component != null) {
            val component = launchIntent.component!!.flattenToShortString()
            "am start -n $component"
        } else {
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        }
        val res = privilegeManager.executeElevated(cmd)
        return res.isSuccess
    }

    private suspend fun smartClickText(targetText: String): Boolean {
        val tmpDumpPath = "/data/local/tmp/autoroid_ui.xml"
        // 1. Dump UI hierarchy
        val dumpRes = privilegeManager.executeElevated("uiautomator dump $tmpDumpPath")
        if (!dumpRes.isSuccess && !dumpRes.stdout.contains("UI hierchary dumped")) {
            // Some devices write to /sdcard/window_dump.xml
            privilegeManager.executeElevated("uiautomator dump /sdcard/window_dump.xml")
        }

        // 2. Read dumped XML
        var xmlRes = privilegeManager.executeElevated("cat $tmpDumpPath")
        if (!xmlRes.isSuccess || xmlRes.stdout.isBlank()) {
            xmlRes = privilegeManager.executeElevated("cat /sdcard/window_dump.xml")
        }

        val xml = xmlRes.stdout
        if (xml.isBlank()) {
            return false
        }

        // 3. Search for node with matching text or content-desc
        // Pattern: <node ... text="..." ... bounds="[x1,y1][x2,y2]"
        val nodePattern = Pattern.compile("<node[^>]+bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"[^>]*>")
        val matcher = nodePattern.matcher(xml)

        while (matcher.find()) {
            val nodeSnippet = matcher.group(0) ?: continue
            val hasText = nodeSnippet.contains("text=\"$targetText\"", ignoreCase = true) ||
                    nodeSnippet.contains("content-desc=\"$targetText\"", ignoreCase = true)

            if (hasText) {
                val x1 = matcher.group(1)?.toIntOrNull() ?: continue
                val y1 = matcher.group(2)?.toIntOrNull() ?: continue
                val x2 = matcher.group(3)?.toIntOrNull() ?: continue
                val y2 = matcher.group(4)?.toIntOrNull() ?: continue

                val centerX = (x1 + x2) / 2
                val centerY = (y1 + y2) / 2

                val tapRes = privilegeManager.executeElevated("input tap $centerX $centerY")
                return tapRes.isSuccess
            }
        }

        return false
    }
}
