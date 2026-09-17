package com.autoroid.app.feature.telephony.schedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoroid.app.AutoroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? AutoroidApp
                    app?.simScheduleManager?.onBoot()
                    app?.imsController?.onBoot()
                    app?.updateManager?.checkForUpdates()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
