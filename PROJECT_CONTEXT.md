# Autoroid - Project Architecture & Technical Context

> **Last Updated:** 2026-09-19  
> **Package ID:** `com.autoroid.app`  
> **Target Android Version:** Android 15 / 16 / 17 (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 29`)  
> **Distribution Model:** Self-hosted / Power User Automations (Not bound by Google Play sandbox restrictions)

---

## 1. Project Philosophy & Core Purpose

**Autoroid** is an elevated-privilege Android automation engine designed for advanced personal workflows, system overrides, and application macros. Unlike conventional automation apps (Tasker, MacroDroid) that rely on brittle Accessibility service overlays or legacy hacks, Autoroid uses:
1. **Direct Elevated IPC**: Talks directly to Android system services (`ITelephony`, `ISubscription`, `IAccessibilityManager`, `IActivityManager`) via **Root (UID 0)** and **Shizuku (UID 2000 ADB)**.
2. **Native C++ Performance**: Compiles native C++17 shared libraries via Android NDK 27 (`libautoroid_native.so`) for sub-millisecond execution and direct Linux kernel interaction.
3. **Dynamic Stealth & Evasion**: Can strip and restore system accessibility registries on-the-fly to bypass aggressive anti-automation detection in modern banking and enterprise apps.
4. **Any-App UI & Macro Automation**: Automates multi-step flows in any third-party app (e.g. Samsung Health workout starting) via Smart UI Click (text/content-desc), screen coordinate tap, and deep intents.

---

## 2. Technology Stack & Build Environment

| Layer | Technology |
| :--- | :--- |
| **Language & Runtime** | Kotlin 2.0.20 + Java 17/19 (`/Library/Java/JavaVirtualMachines/jdk-19.jdk/Contents/Home`) |
| **Native Toolchain** | C++17, Android NDK `27.0.12077973`, CMake `3.22.1` |
| **Android Build** | Android Gradle Plugin `8.6.0`, Gradle `8.10` |
| **UI Framework** | Jetpack Compose Material 3 Dark theme (Cyber Cyan `#00E5FF`, Neon Green `#2EE59D`) |
| **Elevation Backends** | `rikka.shizuku:api:13.1.5`, `rikka.shizuku:provider:13.1.5`, Native/KernelSU/APatch/Magisk `su` |
| **Signing** | Dynamic release signing via gitignored `keystore.properties` / CI secrets (`RELEASE_KEYSTORE_BASE64`) |

### Build Commands:
```bash
# Debug APK
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-19.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew assembleDebug

# Release APK (Signed via keystore.properties or environment variables)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Automated GitHub Actions CI/CD (`.github/workflows/release.yml`):
Whenever a version tag (`v*`) is pushed:
1. Ubuntu runner checks out repo with full history.
2. Installs Temurin JDK 19, NDK 27, and CMake.
3. Compiles and signs the release APK using `autoroid-release.jks`.
4. Uses native `gh release` CLI to idempotently publish or update the release and upload `app-release.apk` with `--clobber`.

---

## 3. Subsystem Architecture

### A. Privilege Engine (`app/src/main/java/com/autoroid/app/core/privilege`)
* **`PrivilegeLevel`**: Enum (`ROOT`, `SHIZUKU`, `ADB`, `NONE`).
* **`PrivilegeEngine`**: Interface defining `suspend fun execute(command: String): CommandResult` and `suspend fun isAvailable(): Boolean`.
* **`RootEngine`**: Executes commands as `su -c ...` (UID 0) across Magisk, KernelSU, and APatch.
* **`ShizukuEngine`**: Executes shell commands with UID 2000 ADB privileges using direct AIDL `IShizukuService.Stub.asInterface(Shizuku.getBinder()).newProcess(...)` with `ParcelFileDescriptor` streaming.
* **`ShizukuProvider`**: Custom provider subclassing `rikka.shizuku.ShizukuProvider`, automatically exempting hidden APIs via `HiddenApiBypass.addHiddenApiExemptions("")` on process bootstrap before `Application.onCreate()`.
* **`PrivilegeManager`**: Dynamic resolution. Automatically checks Root first, falls back to Shizuku, handles runtime disconnects, and coordinates fallback execution.

### B. Native Core (`app/src/main/cpp` & `app/src/main/java/com/autoroid/app/core/native`)
* **`native_engine.cpp`**: Compiled for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
* Functions:
  - `getNativeCoreVersion()`: Identifies native engine architecture.
  - `isDirectRootAvailable()`: High-speed native access checks across common su and KernelSU binary locations.
  - `executeNativeCommand(cmd)`: High-performance `popen` wrapper.
* **`NativeEngine.kt`**: JNI bridge loading `autoroid_native`.

### C. System Controllers (`app/src/main/java/com/autoroid/app/feature`)
1. **Bank Mode (`feature/accessibility/AccessibilityController.kt`)**:
   - **Problem:** Banking apps detect running accessibility services and block logins.
   - **Engaged State Machine:** Distinguishes between "All Clear" (0 services enabled, already safe), "Vulnerable" (running services detected), and "Protected" (services paused by Autoroid).
   - **Solution:** When engaged, snapshots current `enabled_accessibility_services` to private preferences, writes `""` to `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, and sets `accessibility_enabled 0`.
   - **Restoration:** One-tap restore rewires the exact list of previous services back into Android settings and sets `accessibility_enabled 1`.
2. **Dual-SIM Data Switcher (`feature/telephony/TelephonyController.kt`)**:
   - Supports Physical SIM + Physical SIM, and Physical SIM + **eSIM** (`info.isEmbedded`).
   - Resolves active data SIM via `SubscriptionManager.getDefaultDataSubscriptionId()`, `Settings.Global.multi_sim_data_call`, and elevated `dumpsys telephony.registry`.
   - Switches default mobile data using direct Shizuku Binder IPC (`ISub.setDefaultDataSubId(subId)` + `ITelephony.setDataEnabledForReason`), with fallbacks to `multi_sim_data_call` and `cmd phone`.
   - Uses `org.lsposed.hiddenapibypass:hiddenapibypass:4.3` to access hidden telephony APIs on Android 10-16.
3. **Automated Scheduled SIM Data Switcher (`feature/telephony/schedule`)**:
   - **`SimSchedule.kt`**: Circular 24-hour time range calculation (`isTimeInWindow`) handling midnight rollovers (e.g. 12:00 AM – 9:00 AM off-peak bundles, or 11:00 PM – 7:00 AM).
   - **`SimScheduleRepository.kt`**: SharedPreferences persistent storage (`autoroid_sim_schedule`).
   - **`SimScheduleManager.kt`**: Background coordinator. Calculates next transitions, schedules exact alarms via `AlarmManager.setExactAndAllowWhileIdle()`, applies elevated modem switches, and issues notification updates.
   - **`SimScheduleReceiver.kt`**: Wakeful broadcast receiver (`PowerManager.PARTIAL_WAKE_LOCK`) surviving Android Deep Doze.
   - **`BootReceiver.kt`**: Restores alarms upon device reboot (`BOOT_COMPLETED`) and updates.
   - **`SimScheduleCard.kt`**: Embedded dashboard card with master toggle, time pickers, SIM selector chips, and live status badges.
4. **Carrier & IMS Patcher (`feature/telephony/ims`)**:
   - **Problem:** Google limits VoLTE, VoWiFi, and 5G VoNR on Pixel devices in unsupported regions (e.g. Pakistan). Android resets carrier overrides on reboot, and CVE-2025-48617 blocks shell from calling `overrideConfig`.
   - **100% TensorIMS / Turbo IMS Feature Coverage**:
     - *Core Calling & IMS:* VoLTE (`carrier_volte_available_bool`), VoWiFi (`carrier_wfc_ims_available_bool`), VoWiFi Roaming (`carrier_wfc_supports_wifi_only_bool`), VoNR 5G Voice (`vonr_enabled_bool`), VT Video Telephony (`carrier_vt_available_bool`), Supplementary Services UT/USSD (`carrier_supports_ss_over_ut_bool`), and Cross-SIM Calling (`carrier_cross_sim_calling_available_bool`).
     - *Advanced 5G & Status Bar Icons:* Standalone/Non-Standalone modem announcement (`carrier_nr_availabilities_int_array = [1, 2]`), 5G+ Ultra Wideband status bar icon (`FiveGPlusConfig`), 5G signal calibration (`5g_nr_ssrsrp_thresholds_int_array = [-128, -98]`), Enhanced 4G LTE controls, "4G" instead of "LTE" indicator, and "LTE+" CA icon suppression.
     - *Identity & Signaling Overrides:* Custom Carrier Name (`carrier_name_string`) and IMS SIP User-Agent string (`carrier_ims_user_agent_string`).
   - **`ImsConfig.kt`**: Multi-type configuration model generating standard CarrierConfig bundles and custom multi-type bundles.
   - **`ImsRepository.kt`**: SharedPreferences JSON persistence (`autoroid_ims_carrier_config`) for Physical SIM and eSIM independent configs.
   - **Compile-Time Framework Stubs (`:stub` module)**: Provides `ServiceManager`, `IActivityManager`, `IInstrumentationWatcher`, `UiAutomationConnection`, and `ITelephony` stubs with 0 APK footprint.
   - **Two-Tier Privileged Instrumentation**:
     - Primary: `ImsModifier.kt` launches via `IActivityManager.startInstrumentation(..., flags = 8)` using `ServiceManager.getService("activity")` wrapped in `ShizukuBinderWrapper`. Delegates shell identity (`startDelegateShellPermissionIdentity`), overrides carrier configs, and sets persistent modem NVRAM provisioning.
     - Fallback: `BrokerInstrumentation.kt` automatically executes if `ImsModifier` yields an empty result or permission error on restricted Pixel builds, ensuring 100% parity with TensorIMS.
   - **`ImsResetter.kt` & `ImsCapabilityReader.kt`**: Instrumentation runners for carrier configuration wipe, IMS reset, and real-time VoLTE/VoWiFi/VoNR capability inspection.
   - **`ImsController.kt`**: Privileged orchestration engine coordinating Shizuku instrumentation sessions and Root fallbacks.
   - **Reboot Engine:** `BootReceiver` and `AutoroidApp` automatically restore overrides on phone restart without needing Turbo IMS.
   - **`ImsCarrierPatcherCard.kt`**: Microchip cyber card with SIM slot switcher, live status banner, live Shizuku authorization warning, organized expandable feature sections, and 1-tap apply/reset actions.
5. **Quick Settings Tiles (`feature/tiles`)**:
   - `BankModeTileService`: Quick Settings tile displaying real-time protected/running/clean state.
   - `SimSwitchTileService`: Quick Settings tile showing active SIM type and carrier name for 1-tap switching.

### D. Dynamic Workflow Engine (`app/src/main/java/com/autoroid/app/feature/workflow`)
1. **Step Types (`model/WorkflowStep.kt`)**:
   - `LaunchApp(packageName, appLabel)`: Elevated `am start -n <component>` or launcher intent.
   - `SmartClickText(targetText)`: Dumps UI with `uiautomator dump`, parses XML bounds `[x1,y1][x2,y2]` for matching text/content-desc, calculates midpoint, and runs `input tap X Y`.
   - `TapCoordinate(x, y, note)`: Direct screen tap injection (`input tap x y`).
   - `Delay(durationMs)`: Coroutine delay for UI painting.
   - `Swipe(startX, startY, endX, endY, durationMs)`: Gestural inputs.
   - `ShellCommand(command)`: Arbitrary elevated shell scripts.
2. **Smart Event Triggers (`trigger/WorkflowTrigger.kt` & `trigger/WorkflowTriggerManager.kt`)**:
   - Background event monitor listening for battery charger connect/disconnect, screen unlocked/off, battery $\le$ 20%, and Wi-Fi network association.
   - Dispatches matching macros directly into `WorkflowRunner.executeWorkflow()` without manual triggers.
3. **Execution Runner (`runner/WorkflowRunner.kt`)**:
   - Sequential step execution on `Dispatchers.IO`.
   - Exposes reactive `ExecutionState` with step progress and status messaging.
4. **Screen Coordinates Inspector (`runner/PointerLocationHelper.kt`)**:
   - Toggles `settings put system pointer_location 1/0`.
   - Overlays real-time $(X, Y)$ touch coordinates in the Android status bar.
5. **Storage (`repository/WorkflowRepository.kt`)**:
   - Reads/writes `autoroid_workflows.json` in `context.filesDir`.
   - Pre-seeds "Samsung Health: Start Running", "Force Stop App Macro", and "Quick Screen Tap Macro".
6. **UI Components (`ui/`)**:
   - `WorkflowCard`: Live execution cards with expandable step details and trigger badges.
   - `WorkflowEditorDialog`: Step builder, event triggers dropdown, reorder, delete, and configure.
   - `AppPickerDialog`: Searchable installed apps selector.

### E. Background App Freezer & Deep Sleep (`app/src/main/java/com/autoroid/app/feature/freezer`)
* **`FrozenApp.kt` & `AppFreezerRepository.kt`**: Managed app model and JSON persistence in SharedPreferences.
* **`AppFreezerManager.kt`**:
  - Suspends packages via `pm suspend` (disables execution and greys out launcher icon) and restores via `pm unsuspend`.
  - Terminates running processes instantly via elevated `am force-stop`.
  - **Auto-Freeze on Screen Off**: Hooks into display power state broadcast to automatically freeze battery-draining apps when the user locks their device.
* **`AppFreezerCard.kt`**: Cyberpunk card in Shield tab with search, quick toggle, instant kill, and auto-freeze switch.

### F. NetCut Per-App Firewall (`app/src/main/java/com/autoroid/app/feature/netcut`)
* **`NetCutApp.kt` & `NetCutRepository.kt`**: Blocklist data model and SharedPreferences persistence.
* **`NetCutManager.kt`**:
  - VPN-less internet blocking at the Linux kernel level.
  - **Root mode**: Uses `iptables` and `ip6tables` (`OUTPUT -m owner --uid-owner <UID> -j DROP`) for complete network isolation.
  - **Shizuku mode**: Enforces strict policy-level blocking via `cmd netpolicy add restrict-background-blacklist <UID>` and `appops set <package> RUN_IN_BACKGROUND ignore`.
* **`NetCutCard.kt`**: Embedded card in Shield tab with app search, instant cut switch, and clear blocked apps counter.

### G. Shizuku Wireless ADB Auto-Starter (`app/src/main/java/com/autoroid/app/feature/shizuku`)
* **`ShizukuStarterManager.kt`**:
  - Automatically queries active TLS ADB ports via system properties (`adb.tls.port`, `service.adb.tls.port`).
  - Launches Shizuku daemon via direct root `/data/user_de/0/moe.shizuku.privileged.api/bin/shizuku_starter` or local shell loops.
  - Provides diagnostic logs and manual port fallback.
* **`ShizukuStarterCard.kt`**: Privileged daemon starter in Console tab.

### H. In-App Self-Update System (`app/src/main/java/com/autoroid/app/feature/update`)
1. **GitHub Releases Client (`UpdateManager.kt`)**:
   - Queries `https://api.github.com/repos/ScoRpiiTech/autoroid/releases/latest`.
   - Parses semver release tags, markdown notes, and APK assets.
   - Smart local cache: uses `PackageManager.getPackageArchiveInfo()` to verify if the APK is already downloaded, skipping redundant downloads.
2. **Elevated Silent Self-Installation**:
   - **Root:** Direct execution of `pm install -r -d <apkPath>` (UID 0).
   - **Shizuku:** Streams APK bytes to `/data/local/tmp/autoroid-update.apk` via remote shell stdin (`cat`), runs `pm install -r -d`, and deletes temp file (UID 2000 ADB).
   - Automatically restarts the updated application via `am start`.
   - Falls back to `androidx.core.content.FileProvider` for standard PackageInstaller flow if unprivileged.
3. **System Notifications & Background Alerting**:
   - `UpdateManager.kt` registers `autoroid_app_updates` NotificationChannel.
   - Posts heads-up status bar notifications (`🚀 Autoroid vX.Y.Z Available`) with expandable changelog preview and 1-tap open intent.
   - Background check on boot via `BootReceiver.kt` alerting users even before the app is opened.
   - Automatic notification dismissal upon installation or when up to date.
4. **UI Components**:
    - `UpdateBannerCard.kt`: Dynamic banner displaying version, notes, download progress, and contextual 1-tap install.
    - `UpdateStatusDialog.kt`: Modal comparison grid, release changelog viewer, and live download/install spinner.

### I. Interactive Feedback & Feature Guidance (`app/src/main/java/com/autoroid/app/ui`)
1. **Reactive UI Event Channel (`MainViewModel.kt`)**:
   - `_uiEvent = MutableSharedFlow<String>()` delivering real-time user-facing status feedback.
   - Pops Snackbars on the screen for every tap (e.g., privilege warnings, Bank Mode state changes, SIM switch results, workflow steps).
2. **Contextual Feature Guidance Dialogs (`dialogs/FeatureInfoDialog.kt`)**:
   - Comprehensive troubleshooting and architectural guide modals covering Root/Shizuku elevation, Bank Mode security mechanics, dual-SIM hardware constraints, workflow automation, and coordinate calibration.
3. **Smart Badges & Missing Prerequisites**:
   - Amber warnings on `WorkflowCard` when target applications are uninstalled.
   - Dual-SIM card dynamic detection and button disabling when fewer than 2 SIMs are installed.
   - Prominent privilege warning banner when `PrivilegeLevel.NONE`.

---

## 4. Key File Map

```
/Volumes/Data/Projects/Autoroid/
├── README.md                                # Public documentation & feature matrix
├── PROJECT_CONTEXT.md                       # Comprehensive architecture & codebase reference
├── CHANGELOG.md                             # Running history of updates and modifications
├── autoroid-release.jks                     # Production signing keystore (alias: key)
├── settings.gradle.kts                      # Gradle modules definition
├── build.gradle.kts                         # Root Gradle plugins
├── stub/                                    # Compile-only Android framework stubs (IActivityManager, IInstrumentationWatcher)
├── website/                                 # Official landing page (autoroid.scorpiitech.com)
│   ├── index.html                           # Cyber-glassmorphic landing page HTML
│   ├── styles.css                           # Obsidian dark theme & glassmorphic styling
│   ├── app.js                               # Dynamic GitHub Releases API client & tab switcher
│   ├── CNAME                                # Custom domain binding (autoroid.scorpiitech.com)
│   └── assets/logo.png                      # Branding launcher logo
├── app/
│   ├── build.gradle.kts                     # App dependencies, NDK CMake config & signingConfigs
│   └── src/main/
│       ├── AndroidManifest.xml              # Manifest, permissions, activities, & tile services
│       ├── cpp/
│       │   ├── CMakeLists.txt               # CMake 3.22.1 build definition
│       │   └── native_engine.cpp            # NDK 27 native C++ implementation
│       ├── java/com/autoroid/app/
│       │   ├── AutoroidApp.kt               # Application entry & dependency orchestrator
│       │   ├── core/
│       │   │   ├── native/
│       │   │   │   └── NativeEngine.kt      # JNI bridge
│       │   │   └── privilege/
│       │   │       ├── PrivilegeLevel.kt    # Enums and CommandResult
│       │   │       ├── PrivilegeEngine.kt   # Engine interface
│       │   │       ├── RootEngine.kt        # Root su backend
│       │   │       ├── ShizukuEngine.kt     # Shizuku reflection backend
│       │   │       ├── ShizukuProvider.kt   # Custom early hidden API exemption provider
│       │   │       └── PrivilegeManager.kt  # Dynamic dispatcher
│       │   ├── feature/
│       │   │   ├── accessibility/
│       │   │   │   └── AccessibilityController.kt # Bank Mode
│       │   │   ├── freezer/
│       │   │   │   ├── AppFreezerManager.kt       # Elevated pm suspend & am force-stop coordinator
│       │   │   │   ├── model/
│       │   │   │   │   └── FrozenApp.kt           # Frozen app model
│       │   │   │   ├── repository/
│       │   │   │   │   └── AppFreezerRepository.kt# Persistent preferences storage
│       │   │   │   └── ui/
│       │   │   │       └── AppFreezerCard.kt      # App freezer management card
│       │   │   ├── netcut/
│       │   │   │   ├── NetCutManager.kt           # Kernel iptables & netpolicy isolation engine
│       │   │   │   ├── model/
│       │   │   │   │   └── NetCutApp.kt           # Blocked app rule model
│       │   │   │   ├── repository/
│       │   │   │   │   └── NetCutRepository.kt    # Blocked apps persistent storage
│       │   │   │   └── ui/
│       │   │   │       └── NetCutCard.kt          # Per-app firewall control card
│       │   │   ├── shizuku/
│       │   │   │   ├── ShizukuStarterManager.kt   # Wireless ADB port detection & daemon starter
│       │   │   │   └── ui/
│       │   │   │       └── ShizukuStarterCard.kt  # 1-tap daemon starter card
│       │   │   ├── telephony/
│       │   │   │   ├── TelephonyController.kt     # Dual-SIM Switcher
│       │   │   │   ├── ims/
│       │   │   │   │   ├── ImsController.kt       # Carrier config override & boot restorer (privileged instrumentation engine)
│       │   │   │   │   ├── model/
│       │   │   │   │   │   └── ImsConfig.kt       # CarrierConfigManager flags data model
│       │   │   │   │   ├── privileged/
│       │   │   │   │   │   ├── ShellPermissionDelegation.kt # Shell permission delegation wrapper
│       │   │   │   │   │   ├── ImsModifier.kt     # Privileged instrumentation runner for overrides & modem NVRAM
│       │   │   │   │   │   ├── BrokerInstrumentation.kt # Fallback instrumentation runner for pure carrier overrides
│       │   │   │   │   │   ├── ImsResetter.kt     # Privileged instrumentation runner for reset
│       │   │   │   │   │   └── ImsCapabilityReader.kt # Privileged live capability inspection
│       │   │   │   │   ├── repository/
│       │   │   │   │   │   └── ImsRepository.kt   # Persistent per-slot SharedPreferences storage
│       │   │   │   │   └── ui/
│       │   │   │   │       └── ImsCarrierPatcherCard.kt # Cyberpunk Carrier & IMS Patcher card
│       │   │   │   ├── schedule/
│       │   │   │   │   ├── SimScheduleManager.kt  # Scheduling coordinator & exact alarm engine
│       │   │   │   │   ├── model/
│       │   │   │   │   │   └── SimSchedule.kt     # Schedule data model & midnight rollover
│       │   │   │   │   ├── receiver/
│       │   │   │   │   │   ├── BootReceiver.kt    # Boot & package restore receiver
│       │   │   │   │   │   └── SimScheduleReceiver.kt # Wakeful alarm trigger receiver
│       │   │   │   │   └── repository/
│       │   │   │   │       └── SimScheduleRepository.kt # SharedPreferences persistence
│       │   │   │   └── ui/
│       │   │   │       └── DualSimManagerCard.kt  # Unified Modem Manager with manual switch & auto-scheduler
│       │   │   ├── tiles/
│       │   │   │   ├── BankModeTileService.kt     # Quick Settings Bank Mode tile
│       │   │   │   └── SimSwitchTileService.kt    # Quick Settings SIM Switch tile
│       │   │   ├── update/
│       │   │   │   ├── model/
│       │   │   │   │   └── UpdateInfo.kt          # Update models & states
│       │   │   │   ├── ui/
│       │   │   │   │   ├── UpdateBannerCard.kt    # Update notification card
│       │   │   │   │   └── UpdateStatusDialog.kt  # Interactive update comparison & install dialog
│       │   │   │   └── UpdateManager.kt           # GitHub Releases client, cached verification & silent installer
│       │   │   └── workflow/
│       │   │       ├── model/
│       │   │       │   ├── WorkflowStep.kt        # Step models & JSON
│       │   │       │   ├── WorkflowTrigger.kt     # Background event trigger types
│       │   │       │   └── Workflow.kt            # Workflow data model & JSON
│       │   │       ├── runner/
│       │   │       │   ├── WorkflowRunner.kt      # Sequential executor & smart click
│       │   │       │   └── PointerLocationHelper.kt # Coordinate overlay helper
│       │   │       ├── repository/
│       │   │       │   └── WorkflowRepository.kt  # JSON file persistence & seed templates
│       │   │       ├── trigger/
│       │   │       │   └── WorkflowTriggerManager.kt # Background system broadcast event evaluator
│       │   │       └── ui/
│       │   │           ├── WorkflowCard.kt        # UI card with progress & trigger chips
│       │   │           ├── WorkflowEditorDialog.kt# Step & trigger editor modal
│       │   │           └── AppPickerDialog.kt     # Searchable installed apps selector
│       │   └── ui/
│       │       ├── MainActivity.kt          # Host activity & Shizuku permission listener
│       │       ├── MainViewModel.kt         # Reactive state manager (StateFlow)
│       │       ├── navigation/
│       │       │   └── AutoroidNavTab.kt    # Navigation destinations enum (IMS, SHIELD, WORKFLOWS, TERMINAL)
│       │       ├── components/
│       │       │   └── FloatingBottomBar.kt # Floating glassmorphic navigation bar
│       │       ├── dialogs/
│       │       │   ├── FeatureInfoDialog.kt # Interactive help & architectural guide dialogs
│       │       │   └── TimePickerDialog.kt  # Cyberpunk digital clock picker dialog
│       │       ├── screens/
│       │       │   ├── HomeScreen.kt        # Primary host dashboard with live privilege chip & animated transitions
│       │       │   └── tabs/
│       │       │       ├── ImsScreenView.kt # Dedicated IMS Patcher & Dual-SIM manager tab
│       │       │       ├── ShieldScreenView.kt # Dedicated Bank Shield & service inspection tab
│       │       │       ├── WorkflowsScreenView.kt # Dedicated automations & macro feed tab
│       │       │       └── TerminalScreenView.kt # Dedicated elevated console & logs tab
│       │       └── theme/Theme.kt           # Cyber dark & glassmorphism Material 3 theme
│       └── res/
│           ├── xml/
│           │   ├── data_extraction_rules.xml
│           │   └── file_paths.xml           # FileProvider paths for updates
│           ├── values/                      # strings.xml, themes.xml, colors.xml
│           ├── drawable/                    # ic_launcher_background.xml, ic_launcher_monochrome.xml, ic_shield.xml
│           ├── drawable-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ # ic_launcher_foreground.png (Adaptive icon layer)
│           ├── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/   # ic_launcher.png, ic_launcher_round.png (Raster mipmaps)
│           └── mipmap-anydpi-v26/           # ic_launcher.xml, ic_launcher_round.xml (Adaptive icon definitions)
```

