package com.autoroid.app.feature.telephony.schedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.autoroid.app.AutoroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_SCHEDULE = "com.autoroid.app.ACTION_SIM_SCHEDULE_TRIGGER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "autoroid:sim_schedule_wakelock"
        )
        wakeLock?.acquire(10_000) // 10 seconds max

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? AutoroidApp
                app?.simScheduleManager?.evaluateAndApplySchedule()
            } finally {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }
}
