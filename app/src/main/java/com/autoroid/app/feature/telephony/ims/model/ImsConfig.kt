package com.autoroid.app.feature.telephony.ims.model

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager

/**
 * Pixel Telephony configuration keys for NR Advanced (5G+) and signal strength thresholds.
 */
object FiveGPlusConfig {
    const val KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ = "nr_advanced_threshold_bandwidth_khz_int"
    const val KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH = "include_lte_for_nr_advanced_threshold_bandwidth_bool"
    const val KEY_ADDITIONAL_NR_ADVANCED_BANDS = "additional_nr_advanced_bands_int_array"
    const val KEY_5G_ICON_CONFIGURATION = "5g_icon_configuration_string"
    const val KEY_NR_ADVANCED_CAPABLE_PCO_ID = "nr_advanced_capable_pco_id_int"

    const val NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ = 110_000
    const val NR_ICON_CONFIGURATION =
        "connected_mmwave:5G_Plus,connected:5G,connected_rrc_idle:5G," +
                "not_restricted_rrc_idle:5G,not_restricted_rrc_con:5G"

    val additionalNrAdvancedBands = intArrayOf(1, 3, 8, 28, 41, 78, 79)

    fun putOverrides(bundle: Bundle) {
        bundle.putInt(KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ, NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ)
        bundle.putBoolean(KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH, false)
        bundle.putIntArray(KEY_ADDITIONAL_NR_ADVANCED_BANDS, additionalNrAdvancedBands)
        bundle.putString(KEY_5G_ICON_CONFIGURATION, NR_ICON_CONFIGURATION)
        bundle.putInt(KEY_NR_ADVANCED_CAPABLE_PCO_ID, 0)
    }

    fun putOverrides(pb: PersistableBundle) {
        pb.putInt(KEY_NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ, NR_ADVANCED_THRESHOLD_BANDWIDTH_KHZ)
        pb.putBoolean(KEY_INCLUDE_LTE_FOR_NR_ADVANCED_THRESHOLD_BANDWIDTH, false)
        pb.putIntArray(KEY_ADDITIONAL_NR_ADVANCED_BANDS, additionalNrAdvancedBands)
        pb.putString(KEY_5G_ICON_CONFIGURATION, NR_ICON_CONFIGURATION)
        pb.putInt(KEY_NR_ADVANCED_CAPABLE_PCO_ID, 0)
    }
}

/**
 * Represents comprehensive carrier configuration and IMS override flags for a SIM subscription,
 * covering all TensorIMS / Turbo IMS features:
 * - VoLTE, VoWiFi, VoNR, VT, UT
 * - Cross-SIM Calling (using mobile data of 2nd SIM for Wi-Fi calling backhaul)
 * - VoWiFi Roaming toggle
 * - 5G NR SA/NSA availabilities, 5G Thresholds, 5G+ Icon override
 * - Enhanced 4G LTE, Hide LTE+ icon, Show 4G for LTE
 * - Custom Carrier Name & IMS SIP User-Agent override
 */
data class ImsConfig(
    val slotIndex: Int = 0,
    val subscriptionId: Int = -1,

    // Core IMS Voice & Calling
    val volteEnabled: Boolean = true,
    val vowifiEnabled: Boolean = true,
    val vowifiRoamingEnabled: Boolean = false,
    val vonrEnabled: Boolean = true,
    val vtEnabled: Boolean = false,
    val utInterfaceEnabled: Boolean = true,
    val crossSimEnabled: Boolean = true,

    // 5G NR & Advanced Icons
    val fiveGnrEnabled: Boolean = true,
    val fiveGThresholdsEnabled: Boolean = true,
    val fiveGPlusIconEnabled: Boolean = true,

    // 4G LTE & Status Bar Icons
    val enhanced4gLteEnabled: Boolean = true,
    val hideLtePlusIcon: Boolean = false,
    val show4gForLte: Boolean = false,

    // Carrier & SIP Identity Overrides
    val carrierName: String = "",
    val imsUserAgent: String = "",

    // System Settings UI Visibility
    val settingsVisibilityEnabled: Boolean = true,

    // State Tracking
    val isApplied: Boolean = false,
    val lastAppliedTimestamp: Long = 0L
) {

    /**
     * Converts to an Android Bundle for transmission to privileged Instrumentation runners.
     */
    fun toBundle(): Bundle {
        val bundle = Bundle()

        // 1. Carrier Name Override
        if (carrierName.isNotBlank()) {
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
            bundle.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, carrierName)
            bundle.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, ":3")
        }

        // 2. IMS User Agent
        if (imsUserAgent.isNotBlank()) {
            bundle.putString("carrier_ims_user_agent_string", imsUserAgent)
        }

        // 3. VoLTE
        if (volteEnabled) {
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
        }

        // 4. Enhanced 4G LTE / Status Bar Icons
        if (enhanced4gLteEnabled) {
            bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
            bundle.putBoolean(CarrierConfigManager.KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL, true)
            bundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
            bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)
        }
        if (hideLtePlusIcon) {
            bundle.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, true)
        }
        if (show4gForLte) {
            bundle.putBoolean("show_4g_for_lte_data_icon_bool", true)
        }

        // 5. Video Telephony (VT)
        if (vtEnabled) {
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)
        }

        // 6. UT / Supplementary Services over IMS
        if (utInterfaceEnabled) {
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
        }

        // 7. Cross-SIM Calling (Backup Calling over cellular data of opposite SIM)
        if (crossSimEnabled) {
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, true)
            bundle.putBoolean(CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL, true)
        }

        // 8. VoWiFi (Wi-Fi Calling)
        if (vowifiEnabled) {
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true)
            bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
            bundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
            bundle.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
            bundle.putInt("wfc_spn_format_idx_int", 6)
        }

        // 9. VoWiFi Roaming
        if (vowifiRoamingEnabled) {
            bundle.putBoolean("carrier_default_wfc_ims_roaming_enabled_bool", true)
        }

        // 10. VoNR (5G Voice)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && vonrEnabled) {
            bundle.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
            bundle.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
        }

        // 11. 5G NR SA/NSA & Thresholds
        if (fiveGnrEnabled) {
            bundle.putIntArray(
                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                intArrayOf(
                    CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                    CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA
                )
            )
            if (fiveGPlusIconEnabled) {
                FiveGPlusConfig.putOverrides(bundle)
            }
            if (fiveGThresholdsEnabled) {
                bundle.putIntArray(
                    CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                    intArrayOf(-128, -118, -108, -98)
                )
            }
        }

        return bundle
    }

    /**
     * Converts this configuration to an Android PersistableBundle of carrier config keys.
     */
    fun toCarrierConfigBundle(): PersistableBundle {
        val pb = PersistableBundle()

        if (carrierName.isNotBlank()) {
            pb.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
            pb.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, carrierName)
            pb.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING, ":3")
        }

        if (imsUserAgent.isNotBlank()) {
            pb.putString("carrier_ims_user_agent_string", imsUserAgent)
        }

        if (volteEnabled) {
            pb.putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
        }

        if (enhanced4gLteEnabled) {
            pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
            pb.putBoolean(CarrierConfigManager.KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL, true)
            pb.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
            pb.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, false)
        }
        if (hideLtePlusIcon) {
            pb.putBoolean(CarrierConfigManager.KEY_HIDE_LTE_PLUS_DATA_ICON_BOOL, true)
        }
        if (show4gForLte) {
            pb.putBoolean("show_4g_for_lte_data_icon_bool", true)
        }

        if (vtEnabled) {
            pb.putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, true)
        }

        if (utInterfaceEnabled) {
            pb.putBoolean(CarrierConfigManager.KEY_CARRIER_SUPPORTS_SS_OVER_UT_BOOL, true)
        }

        if (crossSimEnabled) {
            pb.putBoolean(CarrierConfigManager.KEY_CARRIER_CROSS_SIM_IMS_AVAILABLE_BOOL, true)
            pb.putBoolean(CarrierConfigManager.KEY_ENABLE_CROSS_SIM_CALLING_ON_OPPORTUNISTIC_DATA_BOOL, true)
        }

        if (vowifiEnabled) {
            pb.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, true)
            pb.putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true)
            pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
            pb.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
            pb.putBoolean("show_wifi_calling_icon_in_status_bar_bool", true)
            pb.putInt("wfc_spn_format_idx_int", 6)
        }

        if (vowifiRoamingEnabled) {
            pb.putBoolean("carrier_default_wfc_ims_roaming_enabled_bool", true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && vonrEnabled) {
            pb.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true)
            pb.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
        }

        if (fiveGnrEnabled) {
            pb.putIntArray(
                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                intArrayOf(
                    CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA,
                    CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA
                )
            )
            if (fiveGPlusIconEnabled) {
                FiveGPlusConfig.putOverrides(pb)
            }
            if (fiveGThresholdsEnabled) {
                pb.putIntArray(
                    CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                    intArrayOf(-128, -118, -108, -98)
                )
            }
        }

        return pb
    }
}
