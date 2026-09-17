package com.autoroid.app.feature.telephony.schedule

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.autoroid.app.R
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.telephony.TelephonyController
import com.autoroid.app.feature.telephony.schedule.model.SimSchedule
import com.autoroid.app.feature.telephony.schedule.receiver.SimScheduleReceiver
import com.autoroid.app.feature.telephony.schedule.repository.SimScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class SimScheduleManager(
    private val context: Context,
    private val repository: SimScheduleRepository,
    private val telephonyController: TelephonyController,
    private val privilegeManager: PrivilegeManager
) {
    val schedule: StateFlow<SimSchedule> = repository.schedule

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "autoroid_sim_schedule"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_START = 2001
        private const val REQUEST_CODE_END = 2002
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SIM Schedule Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when mobile data line automatically switches based on time schedule"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun updateSchedule(newSchedule: SimSchedule) {
        repository.saveSchedule(newSchedule)
        if (newSchedule.isEnabled) {
            scope.launch {
                evaluateAndApplySchedule()
            }
        } else {
            cancelAlarms()
        }
    }

    fun toggleSchedule(enabled: Boolean) {
        val current = schedule.value
        updateSchedule(current.copy(isEnabled = enabled))
    }

    suspend fun onBoot() {
        if (schedule.value.isEnabled) {
            evaluateAndApplySchedule()
        }
    }

    /**
     * Evaluates whether current time is in the schedule window,
     * switches to the corresponding target SIM if needed, and arms the next exact alarm.
     */
    suspend fun evaluateAndApplySchedule() {
        val currentSchedule = schedule.value
        if (!currentSchedule.isEnabled) {
            cancelAlarms()
            return
        }

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val inWindow = currentSchedule.isTimeInWindow(currentHour, currentMinute)
        val targetSubId = if (inWindow) currentSchedule.windowSubId else currentSchedule.defaultSubId

        if (targetSubId > 0) {
            telephonyController.refreshSimState()
            val currentActiveSubId = telephonyController.activeDataSubId.value

            if (currentActiveSubId != targetSubId) {
                val simList = telephonyController.simSlots.value
                val targetSim = simList.firstOrNull { it.subscriptionId == targetSubId }
                val targetLabel = targetSim?.let { "${it.simType} (${it.displayLabel})" } ?: "SubId $targetSubId"

                val result = telephonyController.switchToSubId(targetSubId)
                if (result.isSuccess) {
                    showSwitchNotification(targetLabel, inWindow)
                }
            }
        }

        // Arm alarms for the next transitions
        armNextAlarms(currentSchedule)
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun armNextAlarms(simSchedule: SimSchedule) {
        if (alarmManager == null || !simSchedule.isEnabled) return

        val nextStartMillis = calculateNextOccurrence(simSchedule.startHour, simSchedule.startMinute)
        val nextEndMillis = calculateNextOccurrence(simSchedule.endHour, simSchedule.endMinute)

        // Alarm for window start
        val startIntent = Intent(context, SimScheduleReceiver::class.java).apply {
            action = SimScheduleReceiver.ACTION_TRIGGER_SCHEDULE
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_START,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Alarm for window end
        val endIntent = Intent(context, SimScheduleReceiver::class.java).apply {
            action = SimScheduleReceiver.ACTION_TRIGGER_SCHEDULE
        }
        val endPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextStartMillis, startPending)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextEndMillis, endPending)
        } catch (_: SecurityException) {
            // Fallback if SCHEDULE_EXACT_ALARM not yet permitted
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextStartMillis, startPending)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextEndMillis, endPending)
        }
    }

    private fun cancelAlarms() {
        val startIntent = Intent(context, SimScheduleReceiver::class.java).apply {
            action = SimScheduleReceiver.ACTION_TRIGGER_SCHEDULE
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_START,
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPending != null) {
            alarmManager?.cancel(startPending)
            startPending.cancel()
        }

        val endIntent = Intent(context, SimScheduleReceiver::class.java).apply {
            action = SimScheduleReceiver.ACTION_TRIGGER_SCHEDULE
        }
        val endPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_END,
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPending != null) {
            alarmManager?.cancel(endPending)
            endPending.cancel()
        }
    }

    private fun calculateNextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= now.timeInMillis) {
            // If already passed today, advance to tomorrow
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }

    private fun showSwitchNotification(targetLabel: String, isWindowActive: Boolean) {
        val title = if (isWindowActive) "Scheduled Window Active" else "Scheduled Window Ended"
        val message = "Mobile data automatically switched to $targetLabel."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sim_card)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
