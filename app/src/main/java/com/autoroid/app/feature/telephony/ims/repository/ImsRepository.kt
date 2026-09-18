package com.autoroid.app.feature.telephony.ims.repository

import android.content.Context
import android.content.SharedPreferences
import com.autoroid.app.feature.telephony.ims.model.ImsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class ImsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow<Map<Int, ImsConfig>>(emptyMap())
    val configs: StateFlow<Map<Int, ImsConfig>> = _configs.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        val map = mutableMapOf<Int, ImsConfig>()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            if (key.startsWith(KEY_SLOT_PREFIX) && value is String) {
                val slotIndex = key.removePrefix(KEY_SLOT_PREFIX).toIntOrNull() ?: continue
                val config = deserialize(value, slotIndex)
                map[slotIndex] = config
            }
        }
        _configs.value = map
    }

    fun getConfig(slotIndex: Int, subId: Int = -1): ImsConfig {
        return _configs.value[slotIndex] ?: ImsConfig(
            slotIndex = slotIndex,
            subscriptionId = subId,
            volteEnabled = true,
            vowifiEnabled = true,
            vonrEnabled = true,
            utInterfaceEnabled = true,
            settingsVisibilityEnabled = true,
            vtEnabled = false,
            isApplied = false
        )
    }

    fun saveConfig(config: ImsConfig) {
        val serialized = serialize(config)
        prefs.edit().putString("$KEY_SLOT_PREFIX${config.slotIndex}", serialized).apply()
        val updated = _configs.value.toMutableMap()
        updated[config.slotIndex] = config
        _configs.value = updated
    }

    fun markApplied(slotIndex: Int, applied: Boolean, timestamp: Long = System.currentTimeMillis()) {
        val current = getConfig(slotIndex)
        val updated = current.copy(isApplied = applied, lastAppliedTimestamp = timestamp)
        saveConfig(updated)
    }

    private fun serialize(config: ImsConfig): String {
        return JSONObject().apply {
            put("slotIndex", config.slotIndex)
            put("subId", config.subscriptionId)
            put("volte", config.volteEnabled)
            put("vowifi", config.vowifiEnabled)
            put("vowifiRoaming", config.vowifiRoamingEnabled)
            put("vonr", config.vonrEnabled)
            put("vt", config.vtEnabled)
            put("ut", config.utInterfaceEnabled)
            put("crossSim", config.crossSimEnabled)
            put("fiveGnr", config.fiveGnrEnabled)
            put("fiveGThresholds", config.fiveGThresholdsEnabled)
            put("fiveGPlusIcon", config.fiveGPlusIconEnabled)
            put("enhanced4gLte", config.enhanced4gLteEnabled)
            put("hideLtePlusIcon", config.hideLtePlusIcon)
            put("show4gForLte", config.show4gForLte)
            put("carrierName", config.carrierName)
            put("imsUserAgent", config.imsUserAgent)
            put("settingsVisibility", config.settingsVisibilityEnabled)
            put("isApplied", config.isApplied)
            put("timestamp", config.lastAppliedTimestamp)
        }.toString()
    }

    private fun deserialize(jsonStr: String, slotIndex: Int): ImsConfig {
        return try {
            val obj = JSONObject(jsonStr)
            ImsConfig(
                slotIndex = obj.optInt("slotIndex", slotIndex),
                subscriptionId = obj.optInt("subId", -1),
                volteEnabled = obj.optBoolean("volte", true),
                vowifiEnabled = obj.optBoolean("vowifi", true),
                vowifiRoamingEnabled = obj.optBoolean("vowifiRoaming", false),
                vonrEnabled = obj.optBoolean("vonr", true),
                vtEnabled = obj.optBoolean("vt", false),
                utInterfaceEnabled = obj.optBoolean("ut", true),
                crossSimEnabled = obj.optBoolean("crossSim", true),
                fiveGnrEnabled = obj.optBoolean("fiveGnr", true),
                fiveGThresholdsEnabled = obj.optBoolean("fiveGThresholds", true),
                fiveGPlusIconEnabled = obj.optBoolean("fiveGPlusIcon", true),
                enhanced4gLteEnabled = obj.optBoolean("enhanced4gLte", true),
                hideLtePlusIcon = obj.optBoolean("hideLtePlusIcon", false),
                show4gForLte = obj.optBoolean("show4gForLte", false),
                carrierName = obj.optString("carrierName", ""),
                imsUserAgent = obj.optString("imsUserAgent", ""),
                settingsVisibilityEnabled = obj.optBoolean("settingsVisibility", true),
                isApplied = obj.optBoolean("isApplied", false),
                lastAppliedTimestamp = obj.optLong("timestamp", 0L)
            )
        } catch (_: Exception) {
            ImsConfig(slotIndex = slotIndex)
        }
    }

    companion object {
        private const val PREFS_NAME = "autoroid_ims_carrier_config"
        private const val KEY_SLOT_PREFIX = "slot_config_"
    }
}
