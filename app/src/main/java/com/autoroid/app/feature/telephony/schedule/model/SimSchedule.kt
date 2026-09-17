package com.autoroid.app.feature.telephony.schedule.model

import java.util.Locale

data class SimSchedule(
    val isEnabled: Boolean = false,
    val startHour: Int = 0,    // 0 = 12:00 AM (midnight)
    val startMinute: Int = 0,
    val endHour: Int = 9,      // 9 = 09:00 AM
    val endMinute: Int = 0,
    val windowSubId: Int = -1, // SIM to use during window (e.g. night bundle on eSIM)
    val defaultSubId: Int = -1 // SIM to use for all other hours (e.g. day line on Physical SIM)
) {
    /**
     * Checks if a given hour:minute is within the active schedule window,
     * correctly handling midnight rollover (e.g. 23:00 to 07:00, or 00:00 to 09:00).
     */
    fun isTimeInWindow(hour: Int, minute: Int): Boolean {
        val currentMinutes = hour * 60 + minute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes == endMinutes) {
            // Equal start and end means full 24h window
            true
        } else if (startMinutes < endMinutes) {
            // Standard daytime range (e.g. 09:00 to 17:00, or 00:00 to 09:00)
            currentMinutes in startMinutes until endMinutes
        } else {
            // Crosses midnight (e.g. 23:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val isPm = hour >= 12
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
    }

    val formattedStartTime: String
        get() = formatTime(startHour, startMinute)

    val formattedEndTime: String
        get() = formatTime(endHour, endMinute)
}
