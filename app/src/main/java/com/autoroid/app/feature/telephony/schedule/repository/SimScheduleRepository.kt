package com.autoroid.app.feature.telephony.schedule.repository

import android.content.Context
import android.content.SharedPreferences
import com.autoroid.app.feature.telephony.schedule.model.SimSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SimScheduleRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("autoroid_sim_schedule", Context.MODE_PRIVATE)

    private val _schedule = MutableStateFlow(loadSchedule())
    val schedule: StateFlow<SimSchedule> = _schedule.asStateFlow()

    companion object {
        private const val KEY_ENABLED = "schedule_enabled"
        private const val KEY_START_HOUR = "start_hour"
        private const val KEY_START_MINUTE = "start_minute"
        private const val KEY_END_HOUR = "end_hour"
        private const val KEY_END_MINUTE = "end_minute"
        private const val KEY_WINDOW_SUB_ID = "window_sub_id"
        private const val KEY_DEFAULT_SUB_ID = "default_sub_id"
    }

    private fun loadSchedule(): SimSchedule {
        return SimSchedule(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            startHour = prefs.getInt(KEY_START_HOUR, 0),
            startMinute = prefs.getInt(KEY_START_MINUTE, 0),
            endHour = prefs.getInt(KEY_END_HOUR, 9),
            endMinute = prefs.getInt(KEY_END_MINUTE, 0),
            windowSubId = prefs.getInt(KEY_WINDOW_SUB_ID, -1),
            defaultSubId = prefs.getInt(KEY_DEFAULT_SUB_ID, -1)
        )
    }

    fun saveSchedule(schedule: SimSchedule) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, schedule.isEnabled)
            .putInt(KEY_START_HOUR, schedule.startHour)
            .putInt(KEY_START_MINUTE, schedule.startMinute)
            .putInt(KEY_END_HOUR, schedule.endHour)
            .putInt(KEY_END_MINUTE, schedule.endMinute)
            .putInt(KEY_WINDOW_SUB_ID, schedule.windowSubId)
            .putInt(KEY_DEFAULT_SUB_ID, schedule.defaultSubId)
            .apply()

        _schedule.value = schedule
    }

    fun setEnabled(enabled: Boolean) {
        val current = _schedule.value
        saveSchedule(current.copy(isEnabled = enabled))
    }
}
