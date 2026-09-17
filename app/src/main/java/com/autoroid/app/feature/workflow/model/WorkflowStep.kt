package com.autoroid.app.feature.workflow.model

import org.json.JSONObject

sealed interface WorkflowStep {
    val stepType: String
    val displaySummary: String
    fun toJson(): JSONObject

    data class LaunchApp(
        val packageName: String,
        val appLabel: String = ""
    ) : WorkflowStep {
        override val stepType: String = TYPE
        override val displaySummary: String = "Open ${appLabel.ifBlank { packageName }}"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("package", packageName)
            put("label", appLabel)
        }

        companion object {
            const val TYPE = "LAUNCH_APP"
            fun fromJson(json: JSONObject) = LaunchApp(
                packageName = json.optString("package", ""),
                appLabel = json.optString("label", "")
            )
        }
    }

    data class TapCoordinate(
        val x: Int,
        val y: Int,
        val note: String = ""
    ) : WorkflowStep {
        override val stepType: String = TYPE
        override val displaySummary: String = "Tap at ($x, $y)${if (note.isNotBlank()) " [$note]" else ""}"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("x", x)
            put("y", y)
            put("note", note)
        }

        companion object {
            const val TYPE = "TAP_COORDINATE"
            fun fromJson(json: JSONObject) = TapCoordinate(
                x = json.optInt("x", 0),
                y = json.optInt("y", 0),
                note = json.optString("note", "")
            )
        }
    }

    data class SmartClickText(
        val targetText: String
    ) : WorkflowStep {
        override val stepType: String = TYPE
        override val displaySummary: String = "Click text \"$targetText\""

        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("targetText", targetText)
        }

        companion object {
            const val TYPE = "SMART_CLICK_TEXT"
            fun fromJson(json: JSONObject) = SmartClickText(
                targetText = json.optString("targetText", "")
            )
        }
    }

    data class Delay(
        val durationMs: Long
    ) : WorkflowStep {
        override val stepType: String = TYPE
        override val displaySummary: String = "Wait ${durationMs}ms"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("durationMs", durationMs)
        }

        companion object {
            const val TYPE = "DELAY"
            fun fromJson(json: JSONObject) = Delay(
                durationMs = json.optLong("durationMs", 500L)
            )
        }
    }

    data class Swipe(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int,
        val durationMs: Int = 300
    ) : WorkflowStep {
        override val stepType: String = TYPE
        override val displaySummary: String = "Swipe ($startX, $startY) -> ($endX, $endY)"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("startX", startX)
            put("startY", startY)
            put("endX", endX)
            put("endY", endY)
            put("durationMs", durationMs)
        }

        companion object {
            const val TYPE = "SWIPE"
            fun fromJson(json: JSONObject) = Swipe(
                startX = json.optInt("startX", 0),
                startY = json.optInt("startY", 0),
                endX = json.optInt("endX", 0),
                endY = json.optInt("endY", 0),
                durationMs = json.optInt("durationMs", 300)
            )
        }
    }

    data class ShellCommand(
        val command: String
    ) : WorkflowStep {
        override val stepType: String = TYPE
        override val displaySummary: String = "Execute: $command"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("command", command)
        }

        companion object {
            const val TYPE = "SHELL_COMMAND"
            fun fromJson(json: JSONObject) = ShellCommand(
                command = json.optString("command", "")
            )
        }
    }

    companion object {
        fun parse(json: JSONObject): WorkflowStep? {
            return when (json.optString("type")) {
                LaunchApp.TYPE -> LaunchApp.fromJson(json)
                TapCoordinate.TYPE -> TapCoordinate.fromJson(json)
                SmartClickText.TYPE -> SmartClickText.fromJson(json)
                Delay.TYPE -> Delay.fromJson(json)
                Swipe.TYPE -> Swipe.fromJson(json)
                ShellCommand.TYPE -> ShellCommand.fromJson(json)
                else -> null
            }
        }
    }
}
