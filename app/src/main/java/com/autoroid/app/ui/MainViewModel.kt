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
import kotlinx.coroutines.flow.asSharedFlow
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
    val pausedServicesList: StateFlow<List<String>> = accessibilityController.pausedServicesList
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

    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val uiEvent: kotlinx.coroutines.flow.SharedFlow<String> = _uiEvent.asSharedFlow()

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

            if (level != PrivilegeLevel.NONE) {
                privilegeManager.executeElevated("pm grant com.autoroid.app android.permission.READ_PHONE_STATE")
                privilegeManager.executeElevated("pm grant com.autoroid.app android.permission.WRITE_SECURE_SETTINGS")
            }

            accessibilityController.refreshState()
            telephonyController.refreshSimState()
            pointerLocationHelper.refreshState()
            _isBusy.value = false
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        return try {
            AutoroidApp.instance.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun runWorkflow(workflow: com.autoroid.app.feature.workflow.model.Workflow) {
        viewModelScope.launch {
            if (privilegeLevel.value == PrivilegeLevel.NONE) {
                _uiEvent.emit("⚠️ Elevation Required: Connect Shizuku or grant Root to run workflows.")
                log("Workflow ignored: No elevated privileges.")
                return@launch
            }

            // Check if workflow contains an app launch for an uninstalled app
            val missingAppStep = workflow.steps.filterIsInstance<com.autoroid.app.feature.workflow.model.WorkflowStep.LaunchApp>()
                .firstOrNull { !isPackageInstalled(it.packageName) }
            if (missingAppStep != null) {
                val appName = missingAppStep.appLabel.ifBlank { missingAppStep.packageName }
                _uiEvent.emit("⚠️ Target app \"$appName\" is not installed on this device.")
                log("Workflow warning: App \"$appName\" (${missingAppStep.packageName}) is not installed.")
            } else {
                _uiEvent.emit("⚡ Running workflow: \"${workflow.name}\"...")
            }

            log("Triggering Workflow: \"${workflow.name}\" (${workflow.steps.size} steps)")
            val success = workflowRunner.executeWorkflow(workflow)
            if (success) {
                _uiEvent.emit("✅ Workflow \"${workflow.name}\" completed successfully.")
                log("Workflow \"${workflow.name}\" executed successfully.")
            } else {
                _uiEvent.emit("⚠️ Workflow \"${workflow.name}\" finished with issues.")
                log("Workflow \"${workflow.name}\" finished with issues.")
            }
        }
    }

    fun saveWorkflow(workflow: com.autoroid.app.feature.workflow.model.Workflow) {
        viewModelScope.launch {
            workflowRepository.saveWorkflow(workflow)
            _uiEvent.emit("Saved workflow: \"${workflow.name}\"")
            log("Saved workflow: \"${workflow.name}\"")
        }
    }

    fun deleteWorkflow(id: String) {
        viewModelScope.launch {
            workflowRepository.deleteWorkflow(id)
            _uiEvent.emit("Workflow deleted.")
            log("Deleted workflow.")
        }
    }

    fun togglePointerLocation() {
        viewModelScope.launch {
            if (privilegeLevel.value == PrivilegeLevel.NONE) {
                _uiEvent.emit("⚠️ Elevation Required: Connect Shizuku or grant Root to toggle touch coordinates.")
                log("Pointer Location failed: Neither Root nor Shizuku is connected.")
                return@launch
            }
            val enabled = pointerLocationHelper.toggle()
            if (enabled) {
                _uiEvent.emit("🎯 Touch Coordinates ON: Status bar now shows live (X, Y) touch positions.")
            } else {
                _uiEvent.emit("🎯 Touch Coordinates OFF.")
            }
            log("Screen Coordinate Overlay (Pointer Location): ${if (enabled) "ENABLED" else "DISABLED"}")
        }
    }

    fun toggleBankMode() {
        viewModelScope.launch {
            if (privilegeLevel.value == PrivilegeLevel.NONE) {
                _uiEvent.emit("⚠️ Elevation Required: Connect Shizuku or grant Root to use Bank Mode.")
                log("Bank Mode failed: Neither Root nor Shizuku is connected.")
                return@launch
            }
            _isBusy.value = true
            val willEnable = !isBankModeActive.value
            val servicesCount = activeServicesList.value.size
            val pausedCount = pausedServicesList.value.size

            if (willEnable && servicesCount == 0) {
                _uiEvent.emit("ℹ️ All Clear: No accessibility services are active on your device. Banking apps are already safe!")
                log("Bank Mode: 0 services active, no pausing needed.")
                _isBusy.value = false
                return@launch
            }

            log("Toggling Bank Mode -> ${if (willEnable) "ACTIVATE (Pause Accessibility)" else "RESTORE"}")
            val res = accessibilityController.toggleBankMode()
            if (res.isSuccess) {
                if (willEnable) {
                    _uiEvent.emit("🛡️ Bank Mode Active: $servicesCount accessibility service(s) paused for banking.")
                } else {
                    _uiEvent.emit("✅ Restored $pausedCount accessibility service(s).")
                }
                log("Bank Mode updated successfully.")
            } else {
                val err = res.stderr.ifBlank { "Exit code ${res.exitCode}" }
                _uiEvent.emit("⚠️ Bank Mode failed: $err")
                log("Bank Mode failed: $err")
            }
            _isBusy.value = false
        }
    }

    fun toggleAlternateSim() {
        viewModelScope.launch {
            if (privilegeLevel.value == PrivilegeLevel.NONE) {
                _uiEvent.emit("⚠️ Elevation Required: Connect Shizuku or grant Root to switch SIMs.")
                log("SIM switch failed: Neither Root nor Shizuku is connected.")
                return@launch
            }
            val slots = simSlots.value
            if (slots.size < 2) {
                _uiEvent.emit("ℹ️ Dual-SIM switching requires at least 2 active SIM cards (Physical SIM + eSIM).")
                log("SIM switch ignored: Only ${slots.size} SIM detected.")
                return@launch
            }
            _isBusy.value = true
            val currentSub = activeDataSubId.value
            val target = slots.firstOrNull { it.subscriptionId != currentSub } ?: slots.firstOrNull { !it.isDefaultData } ?: slots[0]
            log("Switching Mobile Data to ${target.simType} (${target.displayLabel}, SubId: ${target.subscriptionId})...")
            _uiEvent.emit("📶 Switching data to ${target.simType} (${target.displayLabel})...")

            val res = telephonyController.switchToSubId(target.subscriptionId)
            if (res.isSuccess) {
                _uiEvent.emit("📶 Active Mobile Data is now on ${target.simType} (${target.displayLabel}).")
                log("Data switch succeeded: ${target.simType} (${target.displayLabel}) is active.")
            } else {
                val err = res.stderr.ifBlank { "Exit code ${res.exitCode}" }
                _uiEvent.emit("⚠️ Data switch failed: $err")
                log("Data switch command failed: $err")
            }
            _isBusy.value = false
        }
    }

    fun switchToSubId(subId: Int) {
        viewModelScope.launch {
            if (privilegeLevel.value == PrivilegeLevel.NONE) {
                _uiEvent.emit("⚠️ Elevation Required: Connect Shizuku or grant Root to switch SIMs.")
                return@launch
            }
            val target = simSlots.value.firstOrNull { it.subscriptionId == subId }
            val label = target?.let { "${it.simType} (${it.displayLabel})" } ?: "SubId $subId"
            _isBusy.value = true
            log("Switching Data SIM to $label")
            _uiEvent.emit("📶 Switching data to $label...")
            val res = telephonyController.switchToSubId(subId)
            if (res.isSuccess) {
                _uiEvent.emit("📶 Mobile data switched to $label.")
                log("Switched to $label.")
            } else {
                val err = res.stderr.ifBlank { "Exit code ${res.exitCode}" }
                _uiEvent.emit("⚠️ SIM switch failed: $err")
                log("SIM switch failed: $err")
            }
            _isBusy.value = false
        }
    }

    fun executeCustomCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            if (privilegeLevel.value == PrivilegeLevel.NONE) {
                _uiEvent.emit("⚠️ Elevation Required: Connect Shizuku or grant Root to run commands.")
                log("[ERR] Elevation required to execute shell commands.")
                return@launch
            }
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
