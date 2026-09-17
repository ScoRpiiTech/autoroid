package com.autoroid.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoroid.app.AutoroidApp
import com.autoroid.app.core.native.NativeEngine
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.feature.accessibility.AccessibilityController
import com.autoroid.app.feature.telephony.SimSlotInfo
import com.autoroid.app.feature.telephony.TelephonyController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {

    private val privilegeManager = AutoroidApp.instance.privilegeManager
    private val accessibilityController = AutoroidApp.instance.accessibilityController
    private val telephonyController = AutoroidApp.instance.telephonyController
    private val workflowRepository = AutoroidApp.instance.workflowRepository
    private val workflowRunner = AutoroidApp.instance.workflowRunner
    private val pointerLocationHelper = AutoroidApp.instance.pointerLocationHelper
    private val updateManager = AutoroidApp.instance.updateManager

    val privilegeLevel: StateFlow<PrivilegeLevel> = privilegeManager.currentLevel
    val isBankModeActive: StateFlow<Boolean> = accessibilityController.isBankModeActive
    val activeServicesList: StateFlow<List<String>> = accessibilityController.activeServicesList
    val simSlots: StateFlow<List<SimSlotInfo>> = telephonyController.simSlots
    val activeDataSubId: StateFlow<Int?> = telephonyController.activeDataSubId
    val workflows: StateFlow<List<com.autoroid.app.feature.workflow.model.Workflow>> = workflowRepository.workflows
    val executionState: StateFlow<com.autoroid.app.feature.workflow.runner.ExecutionState> = workflowRunner.executionState
    val isPointerLocationActive: StateFlow<Boolean> = pointerLocationHelper.isPointerLocationActive
    val updateStatus: StateFlow<com.autoroid.app.feature.update.model.UpdateStatus> = updateManager.updateStatus
    val currentVersion: String = updateManager.currentVersion

    private val _nativeVersion = MutableStateFlow("Initializing...")
    val nativeVersion: StateFlow<String> = _nativeVersion.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        log("Autoroid Engine starting up...")
        loadNativeInfo()
        refreshAll()
    }

    private fun loadNativeInfo() {
        try {
            val ver = NativeEngine.getNativeCoreVersion()
            _nativeVersion.value = ver
            log("Native Bridge: $ver")
        } catch (e: Throwable) {
            _nativeVersion.value = "Native Core fallback (pure ART)"
            log("Native Bridge fallback: ${e.message}")
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isBusy.value = true
            val level = privilegeManager.refresh()
            log("Elevated privilege status: ${level.label}")

            accessibilityController.refreshState()
            telephonyController.refreshSimState()
            pointerLocationHelper.refreshState()
            _isBusy.value = false
        }
    }

    fun runWorkflow(workflow: com.autoroid.app.feature.workflow.model.Workflow) {
        viewModelScope.launch {
            log("Triggering Workflow: \"${workflow.name}\" (${workflow.steps.size} steps)")
            val success = workflowRunner.executeWorkflow(workflow)
            if (success) {
                log("Workflow \"${workflow.name}\" executed successfully.")
            } else {
                log("Workflow \"${workflow.name}\" finished with issues.")
            }
        }
    }

    fun saveWorkflow(workflow: com.autoroid.app.feature.workflow.model.Workflow) {
        viewModelScope.launch {
            workflowRepository.saveWorkflow(workflow)
            log("Saved workflow: \"${workflow.name}\"")
        }
    }

    fun deleteWorkflow(id: String) {
        viewModelScope.launch {
            workflowRepository.deleteWorkflow(id)
            log("Deleted workflow.")
        }
    }

    fun togglePointerLocation() {
        viewModelScope.launch {
            val enabled = pointerLocationHelper.toggle()
            log("Screen Coordinate Overlay (Pointer Location): ${if (enabled) "ENABLED" else "DISABLED"}")
        }
    }

    fun toggleBankMode() {
        viewModelScope.launch {
            _isBusy.value = true
            val willEnable = !isBankModeActive.value
            log("Toggling Bank Mode -> ${if (willEnable) "ACTIVATE (Kill Accessibility)" else "RESTORE"}")
            val res = accessibilityController.toggleBankMode()
            if (res.isSuccess) {
                log("Bank Mode updated successfully.")
            } else {
                log("Bank Mode failed: ${res.stderr.ifBlank { "Exit code ${res.exitCode}" }}")
            }
            _isBusy.value = false
        }
    }

    fun toggleAlternateSim() {
        viewModelScope.launch {
            _isBusy.value = true
            log("Switching Mobile Data to alternate SIM slot...")
            val res = telephonyController.toggleAlternateSim()
            if (res.isSuccess) {
                log("Data switch succeeded.")
            } else {
                log("Data switch command failed: ${res.stderr.ifBlank { "Exit code ${res.exitCode}" }}")
            }
            _isBusy.value = false
        }
    }

    fun switchToSubId(subId: Int) {
        viewModelScope.launch {
            _isBusy.value = true
            log("Switching Data SIM to Subscription ID: $subId")
            val res = telephonyController.switchToSubId(subId)
            if (res.isSuccess) {
                log("Switched to SubId $subId.")
            } else {
                log("Failed switching to SubId $subId: ${res.stderr}")
            }
            _isBusy.value = false
        }
    }

    fun executeCustomCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            log("> $command")
            val res = privilegeManager.executeElevated(command)
            if (res.stdout.isNotBlank()) {
                log(res.stdout)
            }
            if (res.stderr.isNotBlank()) {
                log("[ERR] ${res.stderr}")
            }
            log("Process finished (exit: ${res.exitCode})")
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            log("Checking GitHub (ScoRpiiTech/autoroid) for updates...")
            val info = updateManager.checkForUpdates()
            if (info != null && info.hasUpdate) {
                log("New version available: ${info.latestVersion}")
            } else {
                log("No new updates found.")
            }
        }
    }

    fun downloadAndInstallUpdate(info: com.autoroid.app.feature.update.model.UpdateInfo) {
        viewModelScope.launch {
            log("Downloading update from GitHub: ${info.latestVersion}...")
            updateManager.downloadAndInstall(info)
        }
    }

    fun log(msg: String) {
        val timestamp = timeFormat.format(Date())
        _consoleLogs.value = listOf("[$timestamp] $msg") + _consoleLogs.value.take(49)
    }
}
