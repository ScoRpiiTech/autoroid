package com.autoroid.app.feature.workflow.repository

import android.content.Context
import com.autoroid.app.feature.workflow.model.Workflow
import com.autoroid.app.feature.workflow.model.WorkflowStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.UUID

class WorkflowRepository(private val context: Context) {

    private val storageFile = File(context.filesDir, "autoroid_workflows.json")
    private val _workflows = MutableStateFlow<List<Workflow>>(emptyList())
    val workflows: StateFlow<List<Workflow>> = _workflows.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (!storageFile.exists() || storageFile.length() == 0L) {
            val defaults = createDefaultTemplates()
            saveAll(defaults)
            _workflows.value = defaults
        } else {
            try {
                val jsonString = storageFile.readText()
                val array = JSONArray(jsonString)
                val list = mutableListOf<Workflow>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    list.add(Workflow.fromJson(obj))
                }
                _workflows.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                val defaults = createDefaultTemplates()
                saveAll(defaults)
                _workflows.value = defaults
            }
        }
    }

    suspend fun saveWorkflow(workflow: Workflow) = withContext(Dispatchers.IO) {
        val current = _workflows.value.toMutableList()
        val index = current.indexOfFirst { it.id == workflow.id }
        if (index >= 0) {
            current[index] = workflow
        } else {
            current.add(0, workflow)
        }
        saveAll(current)
        _workflows.value = current
    }

    suspend fun deleteWorkflow(id: String) = withContext(Dispatchers.IO) {
        val current = _workflows.value.filter { it.id != id }
        saveAll(current)
        _workflows.value = current
    }

    suspend fun duplicateWorkflow(id: String) = withContext(Dispatchers.IO) {
        val item = _workflows.value.firstOrNull { it.id == id } ?: return@withContext
        val duplicate = item.copy(
            id = UUID.randomUUID().toString(),
            name = "${item.name} (Copy)",
            createdAt = System.currentTimeMillis()
        )
        saveWorkflow(duplicate)
    }

    private fun saveAll(list: List<Workflow>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        storageFile.writeText(array.toString(2))
    }

    private fun createDefaultTemplates(): List<Workflow> {
        return listOf(
            Workflow(
                name = "Samsung Health: Start Running",
                description = "Opens Samsung Health, navigates to running workout, and starts tracking.",
                steps = listOf(
                    WorkflowStep.LaunchApp(
                        packageName = "com.sec.android.app.shealth",
                        appLabel = "Samsung Health"
                    ),
                    WorkflowStep.Delay(durationMs = 800),
                    WorkflowStep.SmartClickText(targetText = "Running"),
                    WorkflowStep.Delay(durationMs = 500),
                    WorkflowStep.SmartClickText(targetText = "Start")
                )
            ),
            Workflow(
                name = "Force Stop App Macro",
                description = "Terminates any rogue background process via elevated shell command.",
                steps = listOf(
                    WorkflowStep.ShellCommand(
                        command = "am force-stop com.sec.android.app.shealth"
                    )
                )
            ),
            Workflow(
                name = "Quick Screen Tap Macro",
                description = "Automated sample tap at center screen coordinate with pre-delay.",
                steps = listOf(
                    WorkflowStep.Delay(durationMs = 300),
                    WorkflowStep.TapCoordinate(x = 540, y = 1200, note = "Center Tap")
                )
            )
        )
    }
}
