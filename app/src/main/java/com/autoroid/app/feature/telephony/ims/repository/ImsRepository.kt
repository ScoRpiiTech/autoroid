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
            put("vonr", config.vonrEnabled)
            put("ut", config.utInterfaceEnabled)
            put("settingsVisibility", config.settingsVisibilityEnabled)
            put("vt", config.vtEnabled)
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
                vonrEnabled = obj.optBoolean("vonr", true),
                utInterfaceEnabled = obj.optBoolean("ut", true),
                settingsVisibilityEnabled = obj.optBoolean("settingsVisibility", true),
                vtEnabled = obj.optBoolean("vt", false),
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
