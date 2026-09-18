# Autoroid

<p align="center">
  <strong>Elevated Privilege Automation Engine for Modern Android (Android 15 / 16 / 17)</strong>
</p>

---

## ⚡ Overview

**Autoroid** is an elevated-privilege Android automation engine designed for advanced personal workflows, system overrides, and application macros. 

Unlike conventional automation apps that rely on brittle Accessibility service overlays or legacy hacks, Autoroid uses:
1. **Direct Elevated IPC**: Direct communication with Android system services (`ITelephony`, `ISubscription`, `IAccessibilityManager`, `IActivityManager`) via **Root (UID 0)** and **Shizuku (UID 2000 ADB shell)**.
2. **Native C++ Performance**: Sub-millisecond execution powered by a native C++17 shared library compiled with Android NDK 27 (`libautoroid_native.so`).
3. **Carrier & IMS Patcher (Turbo IMS Parity)**: Complete elevated carrier configuration override engine for Google Pixel and modern Android devices, unlocking VoLTE, VoWiFi, VoNR 5G, 5G+ Ultra Wideband, Cross-SIM calling, and custom carrier branding.
4. **Dynamic Bank Mode**: Snapshot and restore system accessibility registries on-the-fly to bypass aggressive anti-automation detection in modern banking and enterprise apps.
5. **Scheduled SIM Data Switcher**: Exact-alarm automated SIM data switching for off-peak night packages with seamless midnight rollover and persistent boot restoration.
6. **Any-App Workflow Engine**: Multi-step macro engine supporting Smart UI Clicks (text/content-desc via `uiautomator`), coordinate tapping, swipe gestures, app launch intents, delays, and elevated shell commands.
7. **In-App Self-Update System**: Integrated with GitHub Releases for 1-click elevated silent self-installation.

---

## 🛠️ Architecture & Tech Stack

| Component | Specification |
| :--- | :--- |
| **Target OS** | Android 10+ (`minSdk = 29`, `targetSdk = 36`, `compileSdk = 36`) |
| **Languages** | Kotlin 2.0.20, C++17, Java 17/19 |
| **UI Framework** | Jetpack Compose Material 3 (Cyber Dark theme) |
| **Native Toolchain** | Android NDK `27.0.12077973`, CMake `3.22.1` |
| **Privilege Backends** | `rikka.shizuku:api:13.1.5`, Native su (`KernelSU`, `APatch`, `Magisk`) |
| **Framework Stubs** | Compile-only `:stub` module (`IActivityManager`, `IInstrumentationWatcher`, `ITelephony`, `ServiceManager`) |

---

## 🚀 Key Features

### 1. Dynamic Dual Privilege Core
* Automatically detects and switches between **Root (UID 0)** and **Shizuku (ADB UID 2000)**.
* Executes elevated shell commands with real-time stdout/stderr streaming via `IShizukuService` IPC and native pipes.

### 2. Bank Mode (Accessibility Shield)
* **Problem**: Many banking and enterprise apps refuse to run or block logins if any Accessibility Service is running.
* **Solution**: One-tap toggle snapshots all active accessibility services to private storage, wipes `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, and turns accessibility off. A second tap completely restores all previous services.
* Includes a dedicated **Quick Settings Tile** for 1-tap toggling from the notification shade.

### 3. Dual-SIM Data Switcher
* Reads active subscriptions from `SubscriptionManager` and executes preferred data switching via elevated `cmd phone set-preferred-data-subId <subId>`.
* Includes a dedicated **Quick Settings Tile** for fast switching.

### 4. Carrier & IMS Patcher (Turbo IMS / TensorIMS Feature Parity)
Unlock VoLTE, VoWiFi, and 5G VoNR on Google Pixel and modern Android devices in unsupported countries and carriers:
* **The Problem**: Google restricts carrier features on Pixel devices in unsupported regions (e.g. Pakistan, India, Latin America, Southeast Asia). On Android 14/15/16 (post-October 2025 security patch / CVE-2025-48617), Android's `CarrierConfigLoader` blocks shell processes (UID 2000 ADB / Shizuku) from calling `overrideConfig()`.
* **The Solution — Two-Tier Privileged Instrumentation**:
  1. **Primary Runner (`ImsModifier`)**: Dynamically started via `IActivityManager.startInstrumentation()` with flag `8` (`INSTR_FLAG_NO_RESTART`) without killing the host app. Calls `startDelegateShellPermissionIdentity(Os.getuid(), null)` (authorized because the caller is an active instrumentation) and safely invokes `CarrierConfigManager.overrideConfig(subId, bundle)`.
  2. **Modem NVRAM Hardware Provisioning**: Injects persistent IMS configuration directly to modem NVRAM via `ProvisioningManager` (`KEY_VOIMS_OPT_IN_STATUS`), `ImsMmTelManager.setAdvancedCallingSettingEnabled(true)`, and `SubscriptionManager` properties (`ENHANCED_4G_MODE_ENABLED`).
  3. **Fallback Runner (`BrokerInstrumentation`)**: Automatic secondary fallback executing pure carrier config overrides if restricted by vendor SELinux rules.
  4. **Early Hidden API Exemption**: Custom `ShizukuProvider` executes `HiddenApiBypass.addHiddenApiExemptions("")` on process bootstrap before `Application.onCreate()`.
  5. **Reboot Persistence Engine**: `BootReceiver` restores carrier overrides immediately upon device restart (`BOOT_COMPLETED`), eliminating the need for separate background patcher apps.

#### Full 16-Feature Coverage Matrix:
| Feature Area | Key / Flag | Description |
| :--- | :--- | :--- |
| **VoLTE** | `carrier_volte_available_bool` | Voice over LTE on 4G networks |
| **VoWiFi** | `carrier_wfc_ims_available_bool` | Native Wi-Fi Calling |
| **VoWiFi Roaming** | `carrier_wfc_supports_wifi_only_bool` | Wi-Fi Calling while roaming internationally |
| **VoNR (5G Voice)** | `vonr_enabled_bool` | Native Voice over 5G New Radio (SA) |
| **Video Telephony (VT)** | `carrier_vt_available_bool` | Carrier ViLTE video calling |
| **Supplementary Services (UT)** | `carrier_supports_ss_over_ut_bool` | USSD, call forwarding, and call waiting over 4G/5G |
| **Cross-SIM Calling** | `carrier_cross_sim_calling_available_bool` | Wi-Fi Calling using secondary SIM's cellular data |
| **5G SA / NSA Modem Announcement** | `carrier_nr_availabilities_int_array` | Explicit baseband modem announcement `[1, 2]` |
| **5G+ / Ultra Wideband Icon** | `FiveGPlusConfig` | Display `5G+` / `5G_UW` indicator on C-Band & mmWave |
| **5G Signal Boundary Calibration** | `5g_nr_ssrsrp_thresholds_int_array` | Calibrate SS-RSRP threshold boundaries to `[-128, -98 dBm]` |
| **Enhanced 4G LTE Controls** | `enhanced_4g_lte_on_by_default_bool` | Enforce default 4G advanced calling state |
| **Display "4G" for LTE** | `show_4g_for_lte_data_icon_bool` | Replaces status bar "LTE" indicator with "4G" |
| **Hide "LTE+" CA Icon** | `hide_lte_plus_data_icon_bool` | Suppresses carrier aggregation badge |
| **Settings Switch Visibility** | `editable_enhanced_4g_lte_bool`, etc. | Unhides VoLTE and Wi-Fi Calling switches in System Settings |
| **Custom Carrier Name** | `carrier_name_string` | Overrides operator display name on lock screen and status bar |
| **IMS SIP User-Agent Override** | `carrier_ims_user_agent_string` | Custom SIP UA header for carrier IMS network bypass |

---

### 5. Automated Scheduled SIM Data Switcher
Automate SIM line switching for off-peak night packages without draining battery:
* **Circular 24-Hour Scheduling**: Handles arbitrary midnight rollovers (e.g., switch to SIM 2 between 11:00 PM and 7:00 AM, or 12:00 AM and 9:00 AM).
* **Exact Alarms via AlarmManager**: Uses `AlarmManager.setExactAndAllowWhileIdle()` with `SimScheduleReceiver` (`PARTIAL_WAKE_LOCK`) to fire reliably even during Android Deep Doze.
* **Boot Restoration**: `BootReceiver` automatically recalculates transitions and schedules exact alarms upon device reboot.
* **Dual-SIM & eSIM Support**: Operates across physical SIM cards and eSIM profiles via elevated `ITelephony` IPC.

---

### 6. Any-App Workflow Engine
Create, edit, reorder, and execute dynamic multi-step automations:
* `LaunchApp`: Elevated activity start or launcher intent.
* `SmartClickText`: Fast XML UI dump parser that locates elements by text or content-description and injects center taps.
* `TapCoordinate`: Direct coordinate touch injection (`input tap X Y`).
* `PointerLocation`: Built-in real-time $(X, Y)$ screen coordinate inspector overlay.
* `Delay`, `Swipe`, and `ShellCommand`: Flexible macro building blocks.

---

### 7. In-App Self-Update
* Checks GitHub Releases API for new releases.
* Displays changelogs in an interactive comparison dialog.
* Supports **silent elevated self-update** (`pm install -r -d`) with auto-restart via Root/Shizuku, as well as standard `FileProvider` fallback.

---

## 📦 Building from Source

### Prerequisites
* JDK 17 or JDK 19
* Android SDK (`build-tools 36.0.0`)
* Android NDK `27.0.12077973` and CMake `3.22.1`

### Build Commands
```bash
# Debug Build
./gradlew assembleDebug

# Release Build
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Signing Configuration
For release signing, create a `keystore.properties` file in the root directory (gitignored):
```properties
storeFile=../your-keystore.jks
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```
Or pass environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

---

## 📄 License
This project is open-source under the Apache License 2.0.
