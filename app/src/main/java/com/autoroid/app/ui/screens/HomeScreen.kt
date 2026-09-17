package com.autoroid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.autoroid.app.ui.dialogs.FeatureHelpType
import com.autoroid.app.ui.dialogs.FeatureInfoDialog
import androidx.compose.material3.OutlinedButton
import com.autoroid.app.feature.update.model.UpdateState
import com.autoroid.app.feature.update.ui.UpdateBannerCard
import com.autoroid.app.feature.update.ui.UpdateStatusDialog
import com.autoroid.app.feature.telephony.ui.SimScheduleCard
import com.autoroid.app.feature.workflow.model.Workflow
import com.autoroid.app.feature.workflow.ui.WorkflowCard
import com.autoroid.app.feature.workflow.ui.WorkflowEditorDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.autoroid.app.ui.MainViewModel
import com.autoroid.app.ui.theme.AmberWarn
import com.autoroid.app.ui.theme.CardBorder
import com.autoroid.app.ui.theme.CardSurface
import com.autoroid.app.ui.theme.CyberCyan
import com.autoroid.app.ui.theme.DeepBackground
import com.autoroid.app.ui.theme.NeonGreen
import com.autoroid.app.ui.theme.NeonRed
import com.autoroid.app.ui.theme.TextPrimary
import com.autoroid.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onRequestShizuku: () -> Unit
) {
    val privilegeLevel by viewModel.privilegeLevel.collectAsState()
    val isBankModeActive by viewModel.isBankModeActive.collectAsState()
    val activeServices by viewModel.activeServicesList.collectAsState()
    val pausedServices by viewModel.pausedServicesList.collectAsState()
    val simSlots by viewModel.simSlots.collectAsState()
    val activeDataSubId by viewModel.activeDataSubId.collectAsState()
    val simSchedule by viewModel.simSchedule.collectAsState()
    val nativeVer by viewModel.nativeVersion.collectAsState()
    val logs by viewModel.consoleLogs.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    val workflows by viewModel.workflows.collectAsState()
    val executionState by viewModel.executionState.collectAsState()
    val isPointerLocationActive by viewModel.isPointerLocationActive.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var activeHelpType by remember { mutableStateOf<FeatureHelpType?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var customCmd by remember { mutableStateOf("") }
    var showCreateWorkflowDialog by remember { mutableStateOf(false) }
    var workflowToEdit by remember { mutableStateOf<Workflow?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CardSurface,
                    contentColor = TextPrimary,
                    actionColor = CyberCyan,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AUTOROID",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = CyberCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "v${viewModel.currentVersion}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp
                        )
                    }
                    IconButton(onClick = { activeHelpType = FeatureHelpType.ENGINE_PRIVILEGE }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Feature Guide & How It Works",
                            tint = CyberCyan
                        )
                    }
                    IconButton(onClick = {
                        showUpdateDialog = true
                        viewModel.checkForUpdates()
                    }) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check for Updates",
                            tint = if (updateStatus.state == UpdateState.AVAILABLE) NeonGreen else CyberCyan
                        )
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBackground
                )
            )
        },
        containerColor = DeepBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Update Banner (GitHub Releases)
            if (updateStatus.updateInfo != null &&
                (updateStatus.state == UpdateState.AVAILABLE ||
                 updateStatus.state == UpdateState.DOWNLOADING ||
                 updateStatus.state == UpdateState.INSTALLING) &&
                dismissedUpdateVersion != updateStatus.updateInfo?.latestVersion
            ) {
                item {
                    UpdateBannerCard(
                        updateStatus = updateStatus,
                        onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(it) },
                        onDismiss = { dismissedUpdateVersion = updateStatus.updateInfo?.latestVersion }
                    )
                }
            }

            // Privilege Status Banner
            item {
                PrivilegeStatusCard(
                    level = privilegeLevel,
                    nativeVer = nativeVer,
                    onRequestShizuku = onRequestShizuku,
                    onHelpClick = { activeHelpType = FeatureHelpType.ENGINE_PRIVILEGE }
                )
            }

            // Milestone 1 Feature: Bank Mode (Accessibility Killer)
            item {
                BankModeCard(
                    isActive = isBankModeActive,
                    activeServices = activeServices,
                    pausedServices = pausedServices,
                    onToggle = { viewModel.toggleBankMode() },
                    onHelpClick = { activeHelpType = FeatureHelpType.BANK_MODE }
                )
            }

            // Milestone 1 Feature: SIM Data Switcher
            item {
                SimSwitcherCard(
                    simSlots = simSlots,
                    activeSubId = activeDataSubId,
                    onToggle = { viewModel.toggleAlternateSim() },
                    onSelectSubId = { viewModel.switchToSubId(it) },
                    onHelpClick = { activeHelpType = FeatureHelpType.SIM_SWITCHER }
                )
            }

            // Milestone 1 Feature: Automated Scheduled SIM Data Switcher
            item {
                SimScheduleCard(
                    schedule = simSchedule,
                    simSlots = simSlots,
                    activeSubId = activeDataSubId,
                    onUpdateSchedule = { viewModel.updateSimSchedule(it) },
                    onToggleSchedule = { viewModel.toggleSimSchedule(it) },
                    onHelpClick = { activeHelpType = FeatureHelpType.SIM_SCHEDULE }
                )
            }

            // Dynamic Workflows Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = "WORKFLOW AUTOMATIONS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CyberCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "One-click multi-step app macros",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { activeHelpType = FeatureHelpType.WORKFLOWS },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Workflows Guide",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { viewModel.togglePointerLocation() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPointerLocationActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isPointerLocationActive) NeonGreen else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPointerLocationActive) "Coords ON" else "Coords",
                                fontSize = 11.sp,
                                color = if (isPointerLocationActive) NeonGreen else TextSecondary
                            )
                        }

                        Button(
                            onClick = { showCreateWorkflowDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(workflows) { workflow ->
                WorkflowCard(
                    workflow = workflow,
                    executionState = executionState,
                    isPackageInstalled = { viewModel.isPackageInstalled(it) },
                    onRun = { viewModel.runWorkflow(workflow) },
                    onEdit = { workflowToEdit = workflow },
                    onDelete = { viewModel.deleteWorkflow(workflow.id) }
                )
            }

            // Power Shell / Quick Command Runner
            item {
                ShellConsoleCard(
                    customCmd = customCmd,
                    onCmdChange = { customCmd = it },
                    onRun = {
                        viewModel.executeCustomCommand(customCmd)
                        customCmd = ""
                    },
                    onHelpClick = { activeHelpType = FeatureHelpType.ENGINE_PRIVILEGE },
                    logs = logs
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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

    if (workflowToEdit != null) {
        WorkflowEditorDialog(
            initialWorkflow = workflowToEdit,
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
            onDownloadAndInstall = { info -> viewModel.downloadAndInstallUpdate(info) },
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

@Composable
fun PrivilegeStatusCard(
    level: PrivilegeLevel,
    nativeVer: String,
    onRequestShizuku: () -> Unit,
    onHelpClick: () -> Unit = {}
) {
    val (statusColor, statusText) = when (level) {
        PrivilegeLevel.ROOT -> Pair(NeonGreen, "ACTIVE (Root UID 0)")
        PrivilegeLevel.SHIZUKU -> Pair(CyberCyan, "ACTIVE (Shizuku ADB UID 2000)")
        PrivilegeLevel.ADB -> Pair(CyberCyan, "ACTIVE (Direct ADB)")
        PrivilegeLevel.NONE -> Pair(NeonRed, "DISCONNECTED (Elevation Required)")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ENGINE STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onHelpClick, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Privilege Guide",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = nativeVer,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )

            if (level == PrivilegeLevel.NONE) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = NeonRed.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Elevation is required. Buttons will not work until Root is granted or Shizuku is connected.",
                            fontSize = 11.sp,
                            color = NeonRed,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestShizuku,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Connect Shizuku / Request Privilege", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BankModeCard(
    isActive: Boolean,
    activeServices: List<String>,
    pausedServices: List<String> = emptyList(),
    onToggle: () -> Unit,
    onHelpClick: () -> Unit = {}
) {
    val isAllClear = !isActive && activeServices.isEmpty()
    val borderColor = when {
        isActive -> NeonGreen
        activeServices.isNotEmpty() -> AmberWarn.copy(alpha = 0.6f)
        else -> NeonGreen.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Bank Mode",
                        tint = if (isActive || isAllClear) NeonGreen else AmberWarn,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Bank Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = when {
                                isActive -> "PROTECTED (${pausedServices.size} Service(s) Paused)"
                                activeServices.isNotEmpty() -> "VULNERABLE (${activeServices.size} Service(s) Active)"
                                else -> "ALL CLEAR (Safe For Banks)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isActive || isAllClear -> NeonGreen
                                else -> AmberWarn
                            }
                        )
                    }
                }

                IconButton(onClick = onHelpClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Bank Mode Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Temporarily flushes accessibility services and registry flags so banking apps cannot detect automation tools or overlays. Restores your exact services when you finish.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (isAllClear) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = NeonGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All Clear: No accessibility services are currently enabled on your phone. Banking apps will not detect or block anything.",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (isActive && pausedServices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Paused Services (${pausedServices.size}) • Ready to restore:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                pausedServices.take(3).forEach { service ->
                    val shortName = service.substringAfterLast("/")
                    Text(
                        text = "• $shortName",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
                if (pausedServices.size > 3) {
                    Text(
                        text = "...and ${pausedServices.size - 3} more",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            if (!isActive && activeServices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Services (${activeServices.size}) • May trigger bank security blocks:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarn
                )
                activeServices.take(3).forEach { service ->
                    val shortName = service.substringAfterLast("/")
                    Text(
                        text = "• $shortName",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AmberWarn
                    )
                }
                if (activeServices.size > 3) {
                    Text(
                        text = "...and ${activeServices.size - 3} more",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onToggle,
                enabled = !isAllClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isActive -> NeonGreen
                        activeServices.isNotEmpty() -> CyberCyan
                        else -> Color.DarkGray
                    },
                    contentColor = if (isAllClear) TextSecondary else Color.Black,
                    disabledContainerColor = Color(0xFF1E242B),
                    disabledContentColor = TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        isActive -> if (pausedServices.isNotEmpty()) "RESTORE ${pausedServices.size} SERVICE(S)" else "RESTORE ACCESSIBILITY SERVICES"
                        activeServices.isNotEmpty() -> "ACTIVATE BANK MODE (PAUSE ${activeServices.size} SERVICES)"
                        else -> "ALL CLEAR • NO SERVICES ACTIVE"
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SimSwitcherCard(
    simSlots: List<com.autoroid.app.feature.telephony.SimSlotInfo>,
    activeSubId: Int?,
    onToggle: () -> Unit,
    onSelectSubId: (Int) -> Unit,
    onHelpClick: () -> Unit = {}
) {
    val alternateSim = simSlots.firstOrNull { it.subscriptionId != activeSubId && !it.isDefaultData }
        ?: simSlots.firstOrNull { it.subscriptionId != activeSubId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SimCard,
                        contentDescription = "SIM Switcher",
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dual-SIM Data Switcher",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Active Data SubId: ${activeSubId ?: "Auto"}",
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }
                }

                IconButton(onClick = onHelpClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "SIM Switcher Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Instant 1-tap mobile data switching between physical SIM and eSIM via elevated telephony IPC, bypassing Android Settings menus.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (simSlots.size < 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AmberWarn.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AmberWarn,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (simSlots.isEmpty())
                                "No SIMs detected yet • Grant Phone permission or connect Shizuku."
                            else
                                "Only 1 SIM detected (${simSlots.first().simType}) • Dual-SIM switcher requires 2 active lines.",
                            fontSize = 11.sp,
                            color = AmberWarn,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (simSlots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                simSlots.forEach { slot ->
                    val isCurrent = (slot.subscriptionId == activeSubId) || slot.isDefaultData
                    Surface(
                        onClick = { onSelectSubId(slot.subscriptionId) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) NeonGreen.copy(alpha = 0.08f) else CardSurface,
                        border = BorderStroke(
                            if (isCurrent) 1.5.dp else 1.dp,
                            if (isCurrent) NeonGreen else CardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Type badge (eSIM vs Physical SIM)
                                Surface(
                                    color = if (slot.isEmbedded) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (slot.isEmbedded) CyberCyan.copy(alpha = 0.4f) else CardBorder),
                                    modifier = Modifier.padding(end = 10.dp)
                                ) {
                                    Text(
                                        text = if (slot.isEmbedded) "eSIM" else "SIM ${if (slot.slotIndex >= 0) slot.slotIndex + 1 else slot.subscriptionId}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (slot.isEmbedded) CyberCyan else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = slot.displayLabel,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${slot.carrierName} • SubId ${slot.subscriptionId}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (isCurrent) {
                                Surface(
                                    color = NeonGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(NeonGreen, androidx.compose.foundation.shape.CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "ACTIVE DATA",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "TAP TO SWITCH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onToggle,
                enabled = simSlots.size >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonText = when {
                    simSlots.size >= 2 && alternateSim != null ->
                        "SWITCH DATA TO ${alternateSim.simType.uppercase()} (${alternateSim.displayLabel.uppercase()})"
                    simSlots.size >= 2 -> "TOGGLE ALTERNATE SIM"
                    else -> "DUAL-SIM REQUIRED (${simSlots.size} DETECTED)"
                }
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ShellConsoleCard(
    customCmd: String,
    onCmdChange: (String) -> Unit,
    onRun: () -> Unit,
    onHelpClick: () -> Unit = {},
    logs: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Console",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Elevated Power Console",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onHelpClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Console Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customCmd,
                    onValueChange = onCmdChange,
                    placeholder = { Text("e.g. cmd phone get-preferred-data-subId", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onRun,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeepBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                LazyColumn {
                    items(logs) { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = when {
                                line.contains("[ERR]") -> NeonRed
                                line.contains(">") -> CyberCyan
                                line.contains("successfully") || line.contains("ACTIVE") -> NeonGreen
                                else -> TextSecondary
                            },
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
