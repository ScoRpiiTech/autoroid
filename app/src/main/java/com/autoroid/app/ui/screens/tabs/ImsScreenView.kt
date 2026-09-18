package com.autoroid.app.ui.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autoroid.app.core.privilege.PrivilegeLevel
import com.autoroid.app.feature.telephony.SimSlotInfo
import com.autoroid.app.feature.telephony.ims.model.ImsConfig
import com.autoroid.app.feature.telephony.ims.ui.ImsCarrierPatcherCard
import com.autoroid.app.feature.telephony.schedule.model.SimSchedule
import com.autoroid.app.feature.telephony.ui.DualSimManagerCard

@Composable
fun ImsScreenView(
    simSlots: List<SimSlotInfo>,
    activeDataSubId: Int?,
    simSchedule: SimSchedule,
    imsConfigs: Map<Int, ImsConfig>,
    isImsApplying: Boolean,
    imsLastResult: String?,
    privilegeLevel: PrivilegeLevel,
    onRequestShizuku: () -> Unit,
    onSaveConfig: (ImsConfig) -> Unit,
    onApplyConfig: (Int, Int, ImsConfig) -> Unit,
    onApplyAll: () -> Unit,
    onResetConfig: (Int, Int) -> Unit,
    onToggleAlternateSim: () -> Unit,
    onSelectSubId: (Int) -> Unit,
    onUpdateSchedule: (SimSchedule) -> Unit,
    onToggleSchedule: (Boolean) -> Unit,
    onImsHelpClick: () -> Unit,
    onSimHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Star Hero Card: Carrier & IMS Patcher
        item {
            ImsCarrierPatcherCard(
                simSlots = simSlots,
                imsConfigs = imsConfigs,
                isApplying = isImsApplying,
                privilegeLevel = privilegeLevel,
                lastResult = imsLastResult,
                onRequestShizuku = onRequestShizuku,
                onSaveConfig = onSaveConfig,
                onApplyConfig = onApplyConfig,
                onApplyAll = onApplyAll,
                onResetConfig = onResetConfig,
                onHelpClick = onImsHelpClick
            )
        }

        // Dual-SIM Manager & Scheduled Automations
        item {
            DualSimManagerCard(
                simSlots = simSlots,
                activeSubId = activeDataSubId,
                schedule = simSchedule,
                onToggleAlternateSim = onToggleAlternateSim,
                onSelectSubId = onSelectSubId,
                onUpdateSchedule = onUpdateSchedule,
                onToggleSchedule = onToggleSchedule,
                onHelpClick = onSimHelpClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
