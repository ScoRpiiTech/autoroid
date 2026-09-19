package com.autoroid.app.feature.workflow.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.util.Log
import com.autoroid.app.feature.workflow.model.Workflow
import com.autoroid.app.feature.workflow.model.WorkflowTrigger
import com.autoroid.app.feature.workflow.repository.WorkflowRepository
import com.autoroid.app.feature.workflow.runner.WorkflowRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkflowTriggerManager(
    private val context: Context,
    private val workflowRepository: WorkflowRepository,
    private val workflowRunner: WorkflowRunner
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isRegistered = false
    private var lastBatteryLevel: Int = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            Log.d(TAG, "Trigger broadcast received: $action")

            when (action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    checkAndExecute { it is WorkflowTrigger.PowerConnected }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    checkAndExecute { it is WorkflowTrigger.PowerDisconnected }
                }
                Intent.ACTION_USER_PRESENT -> {
                    checkAndExecute { it is WorkflowTrigger.ScreenUnlocked }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    checkAndExecute { it is WorkflowTrigger.ScreenOff }
                    scope.launch {
                        try {
                            com.autoroid.app.AutoroidApp.instance.appFreezerManager.onScreenOff()
                        } catch (t: Throwable) {
                            Log.w(TAG, "Screen off auto-freeze note: ${t.message}")
                        }
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        val currentPercent = (level * 100) / scale
                        if (currentPercent != lastBatteryLevel) {
                            lastBatteryLevel = currentPercent
                            checkAndExecute { trigger ->
                                trigger is WorkflowTrigger.BatteryLow && currentPercent <= trigger.thresholdPercent
                            }
                        }
                    }
                }
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val netInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                    if (netInfo != null && netInfo.isConnected) {
                        @Suppress("DEPRECATION")
                        val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)
                        val ssid = wifiInfo?.ssid?.trim('"') ?: ""
                        checkAndExecute { trigger ->
                            if (trigger is WorkflowTrigger.WifiConnected) {
                                trigger.ssid.isBlank() || trigger.ssid.equals(ssid, ignoreCase = true)
                            } else {
                                false
                            }
                        }
                    }
                }
            }
        }
    }

    fun startListening() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        isRegistered = true
        Log.i(TAG, "WorkflowTriggerManager started monitoring background events.")
    }

    fun stopListening() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed unregistering receiver: ${e.message}")
        }
        isRegistered = false
    }

    private fun checkAndExecute(predicate: (WorkflowTrigger) -> Boolean) {
        scope.launch {
            val workflows = workflowRepository.workflows.value
            workflows.forEach { workflow ->
                val hasMatch = workflow.triggers.any(predicate)
                if (hasMatch && !workflowRunner.executionState.value.isRunning) {
                    Log.i(TAG, "Trigger matched! Firing autonomous workflow: ${workflow.name}")
                    workflowRunner.executeWorkflow(workflow)
                }
            }
        }
    }

    companion object {
        private const val TAG = "WorkflowTriggerManager"
    }
}
