package com.autoroid.app.feature.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.autoroid.app.core.privilege.CommandResult
import com.autoroid.app.core.privilege.PrivilegeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

data class SimSlotInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val isDefaultData: Boolean,
    val isEmbedded: Boolean = false
) {
    val simType: String
        get() = if (isEmbedded) "eSIM" else "Physical SIM"

    val displayLabel: String
        get() = if (displayName.isNotBlank() && displayName != "SIM" && displayName != "Card") {
            displayName
        } else if (carrierName.isNotBlank() && carrierName != "Unknown Carrier") {
            carrierName
        } else {
            if (isEmbedded) "eSIM" else "SIM ${if (slotIndex >= 0) slotIndex + 1 else subscriptionId}"
        }
}

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

    companion object {
        private var hiddenApiExempted = false

        fun ensureHiddenApiExempted() {
            if (Build.VERSION.SDK_INT >= 28 && !hiddenApiExempted) {
                try {
                    HiddenApiBypass.addHiddenApiExemptions("")
                    hiddenApiExempted = true
                } catch (_: Throwable) {
                }
            }
        }
    }

    /**
     * Resolves the current default mobile data subscription ID using system APIs,
     * Global Settings, and elevated shell fallbacks.
     */
    suspend fun queryActiveDataSubId(): Int? {
        // 1. Try public SubscriptionManager API
        val defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && defaultSubId > 0) {
            return defaultSubId
        }

        // 2. Try Settings.Global.multi_sim_data_call
        try {
            val globalSub = Settings.Global.getInt(context.contentResolver, "multi_sim_data_call", -1)
            if (globalSub > 0) {
                return globalSub
            }
        } catch (_: Throwable) {
        }

        // 3. Fallback: Shell query for multi_sim_data_call
        val res = privilegeManager.executeElevated("settings get global multi_sim_data_call")
        if (res.isSuccess && res.stdout.isNotBlank()) {
            val parsed = res.stdout.trim().toIntOrNull()
            if (parsed != null && parsed > 0) {
                return parsed
            }
        }

        // 4. Fallback: dumpsys telephony.registry
        val dumpsys = privilegeManager.executeElevated("dumpsys telephony.registry | grep -i defaultDataSubId")
        if (dumpsys.isSuccess && dumpsys.stdout.isNotBlank()) {
            val match = Regex("""defaultDataSubId[=:]\s*(\d+)""").find(dumpsys.stdout)
            val subId = match?.groupValues?.get(1)?.toIntOrNull()
            if (subId != null && subId > 0) {
                return subId
            }
        }

        return null
    }

    @SuppressLint("MissingPermission")
    suspend fun refreshSimState() {
        val currentActiveSubId = queryActiveDataSubId()
        _activeDataSubId.value = currentActiveSubId

        // Method A: Query SubscriptionManager directly if permission is granted
        try {
            val subs = subscriptionManager?.activeSubscriptionInfoList
            if (subs != null && subs.isNotEmpty()) {
                val list = subs.map { info: SubscriptionInfo ->
                    val isEmbedded = try {
                        info.isEmbedded
                    } catch (_: Throwable) {
                        false
                    }
                    val isDefault = (currentActiveSubId == info.subscriptionId) ||
                            (currentActiveSubId == null && info.subscriptionId == SubscriptionManager.getDefaultDataSubscriptionId())

                    SimSlotInfo(
                        slotIndex = info.simSlotIndex,
                        subscriptionId = info.subscriptionId,
                        displayName = info.displayName?.toString()?.trim() ?: "SIM ${info.simSlotIndex + 1}",
                        carrierName = info.carrierName?.toString()?.trim() ?: "Unknown Carrier",
                        isDefaultData = isDefault,
                        isEmbedded = isEmbedded
                    )
                }
                _simSlots.value = list
                return
            }
        } catch (_: SecurityException) {
            // Permission missing, try Shizuku IPC or elevated dumpsys
        }

        // Method B: Query via Shizuku Binder IPC as com.android.shell (bypasses permission checks)
        val shizukuSims = querySimsViaShizuku(currentActiveSubId)
        if (shizukuSims.isNotEmpty()) {
            _simSlots.value = shizukuSims
            return
        }

        // Method C: Parse dumpsys isub via elevated shell
        val dumpsysSims = parseSimsFromDumpsys(currentActiveSubId)
        if (dumpsysSims.isNotEmpty()) {
            _simSlots.value = dumpsysSims
        }
    }

    private fun querySimsViaShizuku(currentActiveSubId: Int?): List<SimSlotInfo> {
        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return emptyList()
            }
            ensureHiddenApiExempted()
            val binder = SystemServiceHelper.getSystemService("isub") ?: return emptyList()
            val wrapped = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("com.android.internal.telephony.ISub\$Stub")
            val iSub = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, wrapped)
            val iSubClass = Class.forName("com.android.internal.telephony.ISub")

            val rawList = try {
                val m = iSubClass.getMethod("getActiveSubscriptionInfoList", String::class.java, String::class.java)
                m.invoke(iSub, "com.android.shell", null)
            } catch (_: NoSuchMethodException) {
                val m = iSubClass.getMethod("getActiveSubscriptionInfoList", String::class.java)
                m.invoke(iSub, "com.android.shell")
            }

            if (rawList is List<*>) {
                val result = mutableListOf<SimSlotInfo>()
                for (item in rawList) {
                    if (item is SubscriptionInfo) {
                        val isEmbedded = try { item.isEmbedded } catch (_: Throwable) { false }
                        val isDefault = (currentActiveSubId == item.subscriptionId)
                        result.add(
                            SimSlotInfo(
                                slotIndex = item.simSlotIndex,
                                subscriptionId = item.subscriptionId,
                                displayName = item.displayName?.toString()?.trim() ?: "SIM ${item.simSlotIndex + 1}",
                                carrierName = item.carrierName?.toString()?.trim() ?: "Unknown Carrier",
                                isDefaultData = isDefault,
                                isEmbedded = isEmbedded
                            )
                        )
                    }
                }
                return result
            }
        } catch (_: Throwable) {
        }
        return emptyList()
    }

    private suspend fun parseSimsFromDumpsys(currentActiveSubId: Int?): List<SimSlotInfo> {
        val result = privilegeManager.executeElevated("dumpsys isub")
        if (!result.isSuccess || result.stdout.isBlank()) return emptyList()

        val output = result.stdout
        val simList = mutableListOf<SimSlotInfo>()

        // Split blocks by SubscriptionInfo entries or lines
        val blocks = output.split(Regex("""(?=\{?m?id=|\{id=|SubscriptionInfo:)"""))
        for (block in blocks) {
            val subIdMatch = Regex("""(?:m?id|subId|mSubId)=(\d+)""").find(block) ?: continue
            val subId = subIdMatch.groupValues[1].toIntOrNull() ?: continue
            if (subId <= 0) continue

            val slotMatch = Regex("""(?:m?simSlotIndex|simSlotIndex)=(-?\d+)""").find(block)
            val slotIndex = slotMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            val displayMatch = Regex("""(?:m?displayName|displayName)=([^\s,;\}]+(?: [^\s,;\}]+)*)""").find(block)
            val displayName = displayMatch?.groupValues?.get(1)?.trim() ?: "SIM $subId"

            val carrierMatch = Regex("""(?:m?carrierName|carrierName)=([^\s,;\}]+(?: [^\s,;\}]+)*)""").find(block)
            val carrierName = carrierMatch?.groupValues?.get(1)?.trim() ?: "Carrier"

            val isEmbedded = block.contains("isEmbedded=true", ignoreCase = true) ||
                    block.contains("mIsEmbedded=true", ignoreCase = true)

            val isDefault = (currentActiveSubId == subId)
            simList.add(
                SimSlotInfo(
                    slotIndex = slotIndex,
                    subscriptionId = subId,
                    displayName = displayName,
                    carrierName = carrierName,
                    isDefaultData = isDefault,
                    isEmbedded = isEmbedded
                )
            )
        }

        return simList.distinctBy { it.subscriptionId }
    }

    /**
     * Switch default data SIM to specified subscription ID using:
     * 1. Shizuku Binder IPC (direct ISub.setDefaultDataSubId & ITelephony)
     * 2. Global Settings (multi_sim_data_call)
     * 3. Elevated cmd phone overrides
     */
    suspend fun switchToSubId(targetSubId: Int): CommandResult {
        var shizukuSuccess = false

        // 1. Shizuku Binder IPC
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ensureHiddenApiExempted()
                val binder = SystemServiceHelper.getSystemService("isub")
                if (binder != null) {
                    val wrapped = ShizukuBinderWrapper(binder)
                    val stubClass = Class.forName("com.android.internal.telephony.ISub\$Stub")
                    val iSub = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, wrapped)
                    val setMethod = Class.forName("com.android.internal.telephony.ISub")
                        .getMethod("setDefaultDataSubId", Int::class.javaPrimitiveType)
                    setMethod.invoke(iSub, targetSubId)
                    shizukuSuccess = true
                }

                // Also ensure user mobile data is enabled for this subId
                val phoneBinder = SystemServiceHelper.getSystemService("phone")
                if (phoneBinder != null) {
                    val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
                    val phoneStub = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                    val iPhone = phoneStub.getMethod("asInterface", IBinder::class.java).invoke(null, wrappedPhone)
                    val iTelephonyClass = Class.forName("com.android.internal.telephony.ITelephony")
                    try {
                        val setData = iTelephonyClass.getMethod(
                            "setDataEnabledForReason",
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            Boolean::class.javaPrimitiveType,
                            String::class.java
                        )
                        setData.invoke(iPhone, targetSubId, 0, true, "com.android.shell")
                    } catch (_: NoSuchMethodException) {
                        try {
                            val setData2 = iTelephonyClass.getMethod(
                                "setUserDataEnabled",
                                Int::class.javaPrimitiveType,
                                Boolean::class.javaPrimitiveType
                            )
                            setData2.invoke(iPhone, targetSubId, true)
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }

        // 2. System Global settings updates via elevated shell
        privilegeManager.executeElevated("settings put global multi_sim_data_call $targetSubId")
        privilegeManager.executeElevated("settings put global multi_sim_data_call_sub $targetSubId")

        // 3. Telephony CLI commands for OEM ROM compatibility
        privilegeManager.executeElevated("cmd phone set-preferred-data-subId $targetSubId")
        privilegeManager.executeElevated("cmd phone set-default-data-subId $targetSubId")

        // 4. Ensure mobile radio is on
        val svcResult = privilegeManager.executeElevated("svc data enable")

        // Wait a moment for modem radio handoff
        delay(600)
        refreshSimState()

        return if (shizukuSuccess) {
            CommandResult(0, "Switched data to SubId $targetSubId via elevated IPC", "")
        } else {
            svcResult
        }
    }

    /**
     * Toggles mobile data to the alternate SIM in dual-SIM setups (Physical SIM + eSIM).
     */
    suspend fun toggleAlternateSim(): CommandResult {
        refreshSimState()
        val slots = _simSlots.value
        val currentSub = _activeDataSubId.value

        if (slots.size < 2) {
            // Fallback toggle between common subIds 1 and 2
            val next = if (currentSub == 1) 2 else 1
            return switchToSubId(next)
        }

        val alternate = slots.firstOrNull { it.subscriptionId != currentSub } ?: slots.firstOrNull { !it.isDefaultData } ?: slots[0]
        return switchToSubId(alternate.subscriptionId)
    }
}
