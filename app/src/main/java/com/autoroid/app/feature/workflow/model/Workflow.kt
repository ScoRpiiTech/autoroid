package com.autoroid.app.feature.workflow.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Workflow(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val steps: List<WorkflowStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description)
        put("createdAt", createdAt)

        val stepsArray = JSONArray()
        steps.forEach { step ->
            stepsArray.put(step.toJson())
        }
        put("steps", stepsArray)
    }

    companion object {
        fun fromJson(json: JSONObject): Workflow {
            val stepsList = mutableListOf<WorkflowStep>()
            val stepsArray = json.optJSONArray("steps")
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.optJSONObject(i)
                    if (stepObj != null) {
                        WorkflowStep.parse(stepObj)?.let { stepsList.add(it) }
                    }
                }
            }

            return Workflow(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Untitled Workflow"),
                description = json.optString("description", ""),
                steps = stepsList,
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
