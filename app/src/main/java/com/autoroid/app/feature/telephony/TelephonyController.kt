package com.autoroid.app.feature.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.autoroid.app.core.privilege.CommandResult
import com.autoroid.app.core.privilege.PrivilegeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SimSlotInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val isDefaultData: Boolean
)

class TelephonyController(
    private val context: Context,
    private val privilegeManager: PrivilegeManager
) {
    private val subscriptionManager: SubscriptionManager? =
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

    private val _simSlots = MutableStateFlow<List<SimSlotInfo>>(emptyList())
    val simSlots: StateFlow<List<SimSlotInfo>> = _simSlots.asStateFlow()

    private val _activeDataSubId = MutableStateFlow<Int?>(null)
    val activeDataSubId: StateFlow<Int?> = _activeDataSubId.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun refreshSimState() {
        var currentPreferredSubId: Int? = null

        // 1. Try querying preferred subId via elevated command
        val result = privilegeManager.executeElevated("cmd phone get-preferred-data-subId")
        if (result.isSuccess && result.stdout.isNotBlank()) {
            val parsed = result.stdout.trim().toIntOrNull()
            if (parsed != null && parsed >= 0) {
                currentPreferredSubId = parsed
            }
        }

        // 2. Query subscription info from system API if readable
        try {
            val subs = subscriptionManager?.activeSubscriptionInfoList
            if (subs != null && subs.isNotEmpty()) {
                val list = subs.map { info: SubscriptionInfo ->
                    val isDefault = (currentPreferredSubId == info.subscriptionId) ||
                            (currentPreferredSubId == null && info.subscriptionId == SubscriptionManager.getDefaultDataSubscriptionId())
                    SimSlotInfo(
                        slotIndex = info.simSlotIndex,
                        subscriptionId = info.subscriptionId,
                        displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                        carrierName = info.carrierName?.toString() ?: "Unknown Carrier",
                        isDefaultData = isDefault
                    )
                }
                _simSlots.value = list
                _activeDataSubId.value = currentPreferredSubId ?: SubscriptionManager.getDefaultDataSubscriptionId()
                return
            }
        } catch (_: SecurityException) {
            // Handled via shell fallback
        }

        // 3. Fallback: Parse via dumpsys telephony.registry
        val dumpsys = privilegeManager.executeElevated("dumpsys telephony.registry | grep -i defaultDataSubId")
        if (dumpsys.isSuccess && dumpsys.stdout.isNotBlank()) {
            val match = Regex("""defaultDataSubId[=:]\s*(\d+)""").find(dumpsys.stdout)
            val subId = match?.groupValues?.get(1)?.toIntOrNull()
            if (subId != null) {
                _activeDataSubId.value = subId
            }
        }
    }

    /**
     * Switch default data SIM to specified subscription ID.
     */
    suspend fun switchToSubId(targetSubId: Int): CommandResult {
        // Modern Android 12+ / 14 / 15 / 16 command
        val cmd1 = "cmd phone set-preferred-data-subId $targetSubId"
        var result = privilegeManager.executeElevated(cmd1)

        // Fallback for older or OEM-specific Android frameworks
        if (!result.isSuccess) {
            val cmd2 = "cmd phone set-default-data-subId $targetSubId"
            result = privilegeManager.executeElevated(cmd2)
        }

        refreshSimState()
        return result
    }

    /**
     * Toggles mobile data to the alternate SIM in dual-SIM setups.
     */
    suspend fun toggleAlternateSim(): CommandResult {
        refreshSimState()
        val slots = _simSlots.value
        if (slots.size < 2) {
            // If API didn't list slots, attempt toggle between SubId 1 and SubId 2
            val current = _activeDataSubId.value ?: 1
            val next = if (current == 1) 2 else 1
            return switchToSubId(next)
        }

        val currentSub = _activeDataSubId.value
        val alternate = slots.firstOrNull { it.subscriptionId != currentSub } ?: slots[0]
        return switchToSubId(alternate.subscriptionId)
    }
}
