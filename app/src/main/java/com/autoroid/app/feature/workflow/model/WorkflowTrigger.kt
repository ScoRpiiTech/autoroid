package com.autoroid.app.feature.workflow.model

import org.json.JSONObject

sealed interface WorkflowTrigger {
    val triggerType: String
    val displaySummary: String
    fun toJson(): JSONObject

    data object PowerConnected : WorkflowTrigger {
        const val TYPE = "POWER_CONNECTED"
        override val triggerType: String = TYPE
        override val displaySummary: String = "⚡ Charger Connected"
        override fun toJson(): JSONObject = JSONObject().apply { put("type", TYPE) }
    }

    data object PowerDisconnected : WorkflowTrigger {
        const val TYPE = "POWER_DISCONNECTED"
        override val triggerType: String = TYPE
        override val displaySummary: String = "🔌 Charger Unplugged"
        override fun toJson(): JSONObject = JSONObject().apply { put("type", TYPE) }
    }

    data object ScreenUnlocked : WorkflowTrigger {
        const val TYPE = "SCREEN_UNLOCKED"
        override val triggerType: String = TYPE
        override val displaySummary: String = "🔓 Screen Unlocked"
        override fun toJson(): JSONObject = JSONObject().apply { put("type", TYPE) }
    }

    data object ScreenOff : WorkflowTrigger {
        const val TYPE = "SCREEN_OFF"
        override val triggerType: String = TYPE
        override val displaySummary: String = "🔒 Screen Turned Off"
        override fun toJson(): JSONObject = JSONObject().apply { put("type", TYPE) }
    }

    data class BatteryLow(val thresholdPercent: Int = 20) : WorkflowTrigger {
        override val triggerType: String = TYPE
        override val displaySummary: String = "🪫 Battery <= $thresholdPercent%"
        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("threshold", thresholdPercent)
        }

        companion object {
            const val TYPE = "BATTERY_LOW"
            fun fromJson(json: JSONObject) = BatteryLow(
                thresholdPercent = json.optInt("threshold", 20)
            )
        }
    }

    data class WifiConnected(val ssid: String = "") : WorkflowTrigger {
        override val triggerType: String = TYPE
        override val displaySummary: String = if (ssid.isBlank()) "🛜 Any Wi-Fi Connected" else "🛜 Wi-Fi ($ssid)"
        override fun toJson(): JSONObject = JSONObject().apply {
            put("type", TYPE)
            put("ssid", ssid)
        }

        companion object {
            const val TYPE = "WIFI_CONNECTED"
            fun fromJson(json: JSONObject) = WifiConnected(
                ssid = json.optString("ssid", "")
            )
        }
    }

    companion object {
        fun parse(json: JSONObject): WorkflowTrigger? {
            return when (json.optString("type")) {
                PowerConnected.TYPE -> PowerConnected
                PowerDisconnected.TYPE -> PowerDisconnected
                ScreenUnlocked.TYPE -> ScreenUnlocked
                ScreenOff.TYPE -> ScreenOff
                BatteryLow.TYPE -> BatteryLow.fromJson(json)
                WifiConnected.TYPE -> WifiConnected.fromJson(json)
                else -> null
            }
        }
    }
}
