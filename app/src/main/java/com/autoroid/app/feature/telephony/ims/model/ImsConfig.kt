package com.autoroid.app.feature.telephony.ims.model

import android.os.PersistableBundle

/**
 * Represents carrier configuration and IMS override flags for a specific SIM subscription.
 */
data class ImsConfig(
    val slotIndex: Int = 0,
    val subscriptionId: Int = -1,
    val volteEnabled: Boolean = true,
    val vowifiEnabled: Boolean = true,
    val vonrEnabled: Boolean = true,
    val utInterfaceEnabled: Boolean = true,
    val settingsVisibilityEnabled: Boolean = true,
    val vtEnabled: Boolean = false,
    val isApplied: Boolean = false,
    val lastAppliedTimestamp: Long = 0L
) {
    /**
     * Converts this configuration to an Android PersistableBundle of carrier config keys.
     */
    fun toCarrierConfigBundle(): PersistableBundle {
        val bundle = PersistableBundle()

        // 1. VoLTE (Voice over 4G LTE)
        bundle.putBoolean("carrier_volte_available_bool", volteEnabled)
        bundle.putBoolean("enhanced_4g_lte_on_by_default_bool", volteEnabled)
        bundle.putBoolean("editable_enhanced_4g_lte_bool", settingsVisibilityEnabled)
        bundle.putBoolean("hide_enhanced_4g_lte_bool", !settingsVisibilityEnabled)

        // 2. VoWiFi (Wi-Fi Calling)
        bundle.putBoolean("carrier_wfc_ims_available_bool", vowifiEnabled)
        bundle.putBoolean("carrier_default_wfc_ims_enabled_bool", vowifiEnabled)
        bundle.putBoolean("carrier_default_wfc_ims_roaming_enabled_bool", vowifiEnabled)
        bundle.putBoolean("editable_wfc_mode_bool", settingsVisibilityEnabled)
        bundle.putBoolean("carrier_wfc_supports_wifi_only_bool", vowifiEnabled)

        // 3. VoNR (Voice over 5G New Radio)
        bundle.putBoolean("vonr_enabled_bool", vonrEnabled)
        bundle.putBoolean("carrier_vonr_available_bool", vonrEnabled)
        bundle.putBoolean("vonr_setting_visibility_bool", settingsVisibilityEnabled)

        // 4. Ut Interface (USSD / Supplementary Services over IMS)
        bundle.putBoolean("carrier_supports_ss_over_ut_bool", utInterfaceEnabled)

        // 5. Video Telephony (VT)
        bundle.putBoolean("carrier_vt_available_bool", vtEnabled)

        return bundle
    }

    /**
     * Returns a list of key-value pairs suitable for elevated shell execution
     * via: `cmd phone cc set-value -s <slotId> <key> <value>`
     */
    fun toKeyValuePairs(): List<Pair<String, Boolean>> {
        return listOf(
            "carrier_volte_available_bool" to volteEnabled,
            "enhanced_4g_lte_on_by_default_bool" to volteEnabled,
            "editable_enhanced_4g_lte_bool" to settingsVisibilityEnabled,
            "hide_enhanced_4g_lte_bool" to !settingsVisibilityEnabled,
            "carrier_wfc_ims_available_bool" to vowifiEnabled,
            "carrier_default_wfc_ims_enabled_bool" to vowifiEnabled,
            "carrier_default_wfc_ims_roaming_enabled_bool" to vowifiEnabled,
            "editable_wfc_mode_bool" to settingsVisibilityEnabled,
            "carrier_wfc_supports_wifi_only_bool" to vowifiEnabled,
            "vonr_enabled_bool" to vonrEnabled,
            "carrier_vonr_available_bool" to vonrEnabled,
            "vonr_setting_visibility_bool" to settingsVisibilityEnabled,
            "carrier_supports_ss_over_ut_bool" to utInterfaceEnabled,
            "carrier_vt_available_bool" to vtEnabled
        )
    }
}
