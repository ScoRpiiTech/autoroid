package com.autoroid.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.feature.update.model.UpdateState
import com.autoroid.app.feature.update.ui.UpdateBannerCard
import com.autoroid.app.feature.update.ui.UpdateStatusDialog
import com.autoroid.app.feature.workflow.model.Workflow
import com.autoroid.app.feature.workflow.ui.WorkflowEditorDialog
import com.autoroid.app.ui.MainViewModel
import com.autoroid.app.ui.components.FloatingBottomBar
import com.autoroid.app.ui.dialogs.FeatureHelpType
import com.autoroid.app.ui.dialogs.FeatureInfoDialog
import com.autoroid.app.ui.navigation.AutoroidNavTab
import com.autoroid.app.ui.screens.tabs.ImsScreenView
import com.autoroid.app.ui.screens.tabs.ShieldScreenView
import com.autoroid.app.ui.screens.tabs.TerminalScreenView
import com.autoroid.app.ui.screens.tabs.WorkflowsScreenView
import com.autoroid.app.ui.theme.AmberWarn
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DarkSurface
import com.autoroid.app.ui.theme.DeepBackground
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.NeonRed
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onRequestShizuku: () -> Unit,
    initialShowUpdateDialog: Boolean = false
) {
    var currentTab by remember { mutableStateOf(AutoroidNavTab.IMS) }

    val privilegeLevel by viewModel.privilegeLevel.collectAsState()
    val isBankModeActive by viewModel.isBankModeActive.collectAsState()
    val activeServices by viewModel.activeServicesList.collectAsState()
    val pausedServices by viewModel.pausedServicesList.collectAsState()
    val simSlots by viewModel.simSlots.collectAsState()
    val activeDataSubId by viewModel.activeDataSubId.collectAsState()
    val simSchedule by viewModel.simSchedule.collectAsState()
    val imsConfigs by viewModel.imsConfigs.collectAsState()
    val isImsApplying by viewModel.isImsApplying.collectAsState()
    val imsLastResult by viewModel.imsLastResult.collectAsState()
    val nativeVer by viewModel.nativeVersion.collectAsState()
    val logs by viewModel.consoleLogs.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    val workflows by viewModel.workflows.collectAsState()
    val executionState by viewModel.executionState.collectAsState()
    val isPointerLocationActive by viewModel.isPointerLocationActive.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    val frozenApps by viewModel.frozenApps.collectAsState()
    val isFreezerBusy by viewModel.isFreezerBusy.collectAsState()
    val netCutApps by viewModel.netCutApps.collectAsState()
    val isNetCutBusy by viewModel.isNetCutBusy.collectAsState()
    val isShizukuDaemonRunning by viewModel.isShizukuDaemonRunning.collectAsState()
    val wirelessAdbPort by viewModel.wirelessAdbPort.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var activeHelpType by remember { mutableStateOf<FeatureHelpType?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showCreateWorkflowDialog by remember { mutableStateOf(false) }
    var workflowToEdit by remember { mutableStateOf<Workflow?>(null) }
    var showUpdateDialog by remember { mutableStateOf(initialShowUpdateDialog) }
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }

    val privilegeColor = when (privilegeLevel) {
        PrivilegeLevel.ROOT -> NeonGreen
        PrivilegeLevel.SHIZUKU -> CyberCyan
        PrivilegeLevel.ADB -> AmberWarn
        PrivilegeLevel.NONE -> NeonRed
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CardSurface,
                    contentColor = TextPrimary,
                    actionColor = CyberCyan,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "AUTOROID",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 18.sp,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        // Live Privilege Indicator Chip
                        Surface(
                            color = privilegeColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, privilegeColor.copy(alpha = 0.45f)),
                            modifier = Modifier.clickable {
                                if (privilegeLevel == PrivilegeLevel.NONE) {
                                    onRequestShizuku()
                                } else {
                                    activeHelpType = FeatureHelpType.ENGINE_PRIVILEGE
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(privilegeColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = privilegeLevel.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = privilegeColor
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp)
                                .padding(end = 4.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp
                        )
                    }

                    // System Help Guide
                    IconButton(onClick = { activeHelpType = FeatureHelpType.ENGINE_PRIVILEGE }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Feature Guide",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Check for Updates
                    IconButton(onClick = {
                        showUpdateDialog = true
                        viewModel.checkForUpdates()
                    }) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check for Updates",
                            tint = if (updateStatus.state == UpdateState.AVAILABLE) NeonGreen else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Refresh
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBackground
                )
            )
        },
        bottomBar = {
            FloatingBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        containerColor = DeepBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Persistent Update Banner if an update is available
                if (updateStatus.updateInfo != null &&
                    (updateStatus.state == UpdateState.AVAILABLE ||
                     updateStatus.state == UpdateState.DOWNLOADING ||
                     updateStatus.state == UpdateState.INSTALLING) &&
                    dismissedUpdateVersion != updateStatus.updateInfo?.latestVersion
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        UpdateBannerCard(
                            updateStatus = updateStatus,
                            onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(it) },
                            onDismiss = { dismissedUpdateVersion = updateStatus.updateInfo?.latestVersion }
                        )
                    }
                }

                // Smooth Tab Transition Animation
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "tabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when (targetTab) {
                        AutoroidNavTab.IMS -> {
                            ImsScreenView(
                                simSlots = simSlots,
                                activeDataSubId = activeDataSubId,
                                simSchedule = simSchedule,
                                imsConfigs = imsConfigs,
                                isImsApplying = isImsApplying,
                                imsLastResult = imsLastResult,
                                privilegeLevel = privilegeLevel,
                                onRequestShizuku = onRequestShizuku,
                                onSaveConfig = { viewModel.saveImsConfig(it) },
                                onApplyConfig = { slotIndex, subId, config ->
                                    viewModel.applyImsConfig(slotIndex, subId, config)
                                },
                                onApplyAll = { viewModel.applyImsToAllActiveSims() },
                                onResetConfig = { slotIndex, subId ->
                                    viewModel.resetImsConfig(slotIndex, subId)
                                },
                                onToggleAlternateSim = { viewModel.toggleAlternateSim() },
                                onSelectSubId = { viewModel.switchToSubId(it) },
                                onUpdateSchedule = { viewModel.updateSimSchedule(it) },
                                onToggleSchedule = { viewModel.toggleSimSchedule(it) },
                                onImsHelpClick = { activeHelpType = FeatureHelpType.CARRIER_IMS_PATCHER },
                                onSimHelpClick = { activeHelpType = FeatureHelpType.SIM_SWITCHER }
                            )
                        }

                        AutoroidNavTab.SHIELD -> {
                            ShieldScreenView(
                                isActive = isBankModeActive,
                                activeServices = activeServices,
                                pausedServices = pausedServices,
                                frozenApps = frozenApps,
                                isFreezerBusy = isFreezerBusy,
                                onFreezeApp = { viewModel.freezeApp(it) },
                                onUnfreezeApp = { viewModel.unfreezeApp(it) },
                                onSetAutoFreeze = { pkg, enabled -> viewModel.setAutoFreeze(pkg, enabled) },
                                onLaunchApp = { viewModel.launchApp(it) },
                                onRefreshFreezer = { viewModel.refreshFreezerApps() },
                                netCutApps = netCutApps,
                                isNetCutBusy = isNetCutBusy,
                                onToggleNetCut = { pkg, blocked -> viewModel.setInternetBlocked(pkg, blocked) },
                                onRefreshNetCut = { viewModel.refreshNetCutApps() },
                                onToggle = { viewModel.toggleBankMode() },
                                onHelpClick = { activeHelpType = FeatureHelpType.BANK_MODE }
                            )
                        }

                        AutoroidNavTab.WORKFLOWS -> {
                            WorkflowsScreenView(
                                workflows = workflows,
                                executionState = executionState,
                                isPointerLocationActive = isPointerLocationActive,
                                isPackageInstalled = { viewModel.isPackageInstalled(it) },
                                onRunWorkflow = { viewModel.runWorkflow(it) },
                                onEditWorkflow = { workflowToEdit = it },
                                onDeleteWorkflow = { viewModel.deleteWorkflow(it) },
                                onNewWorkflowClick = { showCreateWorkflowDialog = true },
                                onTogglePointerLocation = { viewModel.togglePointerLocation() },
                                onHelpClick = { activeHelpType = FeatureHelpType.WORKFLOWS }
                            )
                        }

                        AutoroidNavTab.TERMINAL -> {
                            TerminalScreenView(
                                privilegeLevel = privilegeLevel,
                                nativeVer = nativeVer,
                                logs = logs,
                                isShizukuRunning = isShizukuDaemonRunning,
                                isShizukuInstalled = viewModel.isShizukuInstalled(),
                                wirelessAdbPort = wirelessAdbPort,
                                onStartShizuku = { viewModel.startShizukuDaemon() },
                                onOpenShizuku = { viewModel.openShizukuApp() },
                                onExecuteCommand = { viewModel.executeCustomCommand(it) },
                                onClearLogs = { viewModel.clearConsoleLogs() },
                                onRequestShizuku = onRequestShizuku,
                                onHelpClick = { activeHelpType = FeatureHelpType.ENGINE_PRIVILEGE }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showCreateWorkflowDialog) {
        WorkflowEditorDialog(
            initialWorkflow = null,
            onDismiss = { showCreateWorkflowDialog = false },
            onSave = {
                viewModel.saveWorkflow(it)
                showCreateWorkflowDialog = false
            }
        )
    }

    workflowToEdit?.let { workflow ->
        WorkflowEditorDialog(
            initialWorkflow = workflow,
            onDismiss = { workflowToEdit = null },
            onSave = {
                viewModel.saveWorkflow(it)
                workflowToEdit = null
            }
        )
    }

    if (showUpdateDialog) {
        UpdateStatusDialog(
            currentVersion = viewModel.currentVersion,
            updateStatus = updateStatus,
            onCheckAgain = { viewModel.checkForUpdates() },
            onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(it) },
            onDismiss = { showUpdateDialog = false }
        )
    }

    activeHelpType?.let { helpType ->
        FeatureInfoDialog(
            helpType = helpType,
            onDismiss = { activeHelpType = null }
        )
    }
}
