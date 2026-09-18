# Autoroid - Development Changelog & Update History

> **DEVELOPER GUIDELINES & HISTORY TRACKING:**  
> Always append any newly added features, modified files, architectural changes, or build updates to the bottom of this file. Include timestamp, change summary, files touched, and verification status.

---

## [v1.0.0] - Initial Architecture & Core Privilege Engine
* **Date:** 2026-09-17
* **Status:** Verified (Build Successful, Release APK Signed)

### 1. Architectural Setup & Tooling
* Established modern Android project targeting Android 15 / 16 / 17 (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 29`).
* Configured Gradle 8.10 + AGP 8.6.0 + Kotlin 2.0.20 + NDK `27.0.12077973` with CMake 3.22.1.
* Configured production release signing keys (APK Signature Scheme v3).

### 2. Dual Privilege Core
* **`PrivilegeLevel.kt`**: Defines `ROOT`, `SHIZUKU`, `ADB`, and `NONE`.
* **`RootEngine.kt`**: Interactive `su -c` execution (UID 0) supporting KernelSU, APatch, and Magisk.
* **`ShizukuEngine.kt`**: Remote reflection process execution over Shizuku Binder (UID 2000 ADB shell).
* **`PrivilegeManager.kt`**: Dynamic priority dispatcher resolving Root $\to$ Shizuku $\to$ ADB with fallback.

### 3. Native C++ Core
* **`native_engine.cpp`**: Compiled for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` into `libautoroid_native.so`.
* High-speed native su binary detection and fast `popen` shell execution.

### 4. Milestone 1 System Features
* **Bank Mode (`AccessibilityController.kt`)**: Automatically snapshots running accessibility services, wipes `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` and `accessibility_enabled` to bypass banking app security blocks, and restores state on demand.
* **SIM Data Switcher (`TelephonyController.kt`)**: Instant mobile data line failover via elevated `cmd phone set-preferred-data-subId <subId>`.
* **Quick Settings Tiles**:
  - `BankModeTileService.kt`: One-tap protection toggle from the notification shade.
  - `SimSwitchTileService.kt`: One-tap data SIM toggle from the notification shade.

---

## [v1.1.0] - Dynamic Workflow Engine & App Macro Automation
* **Date:** 2026-09-17
* **Status:** Verified (Release APK: 40MB, Signed with APK Signature Scheme v3)

### 1. Dynamic Any-App Workflow Engine
* **`WorkflowStep.kt`**: Polymorphic steps supporting `LaunchApp`, `SmartClickText`, `TapCoordinate`, `Delay`, `Swipe`, and `ShellCommand`.
* **`Workflow.kt`**: Workflow model with full JSON serialization.
* **`WorkflowRunner.kt`**: Sequential background executor with live progress tracking and UI hierarchy parser (`uiautomator dump` $\to$ text bounds parser $\to$ center tap injection).
* **`WorkflowRepository.kt`**: Persistent storage in `autoroid_workflows.json` with pre-seeded templates:
  - *Samsung Health: Start Running* (`LaunchApp` $\to$ `Delay` $\to$ `SmartClickText("Running")` $\to$ `Delay` $\to$ `SmartClickText("Start")`).
  - *Force Stop App Macro*.
  - *Quick Screen Tap Macro*.

### 2. Screen Coordinates Inspector (Pointer Location)
* **`PointerLocationHelper.kt`**: 1-tap toggle for `settings put system pointer_location 1/0`. Displays real-time $(X, Y)$ touch coordinates in the Android status bar for easy screen tap calibration.

### 3. Interactive UI Components
* **`WorkflowCard.kt`**: Live execution cards with expandable step lists and real-time step execution spinners.
* **`WorkflowEditorDialog.kt`**: Visual modal to create, edit, reorder, and configure workflow steps.
* **`AppPickerDialog.kt`**: Searchable list of all installed apps to pick targets without manual package typing.
* **`HomeScreen.kt`**: Integrated Workflows section, Pointer Location toggle button, and dialog bindings.

### Build Outputs:
* `app/build/outputs/apk/release/app-release.apk` (40 MB, signed with `autoroid-release.jks`, v3 verified).

---

## [v1.2.0] - In-App Self-Update System & GitHub Integration
* **Date:** 2026-09-17
* **Status:** Verified (Release APK: 40MB, Signed with APK Signature Scheme v3)

### 1. In-App Self-Update Engine
* **`UpdateInfo.kt`**: Update metadata model (`UpdateInfo`, `UpdateState`, `UpdateStatus`).
* **`UpdateManager.kt`**:
  - Automatically queries GitHub Releases API (`https://api.github.com/repos/ScoRpiiTech/autoroid/releases/latest`).
  - Identifies newer releases, fetches APK download asset URL and release notes.
  - Streams APK file to internal cache with live percentage progress.
  - **Elevated 1-Click Silent Install**: Automatically runs `pm install -r -d <apkPath>` via Root or Shizuku without interrupting the user with system prompts, then automatically restarts the app!
  - Standard Android `FileProvider` fallback when elevated access is not present.
* **`file_paths.xml`**: Added Android FileProvider configuration for cached update APKs.
* **`AndroidManifest.xml`**: Added `INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions and FileProvider declaration.

### 2. UI Updates
* **`UpdateBannerCard.kt`**: Sleek update notification banner displaying new release version, changelog summary, progress bar, and 1-tap "Download & Self-Install" button.
* **`HomeScreen.kt`**: Bound update banner, and added TopAppBar update check icon button.
* **`MainViewModel.kt`**: Added `checkForUpdates()` and `downloadAndInstallUpdate()`.

### 3. Git Repository & Automated CI/CD Release Pipeline
* Initialized local Git repository on `main` branch.
* Created `.gitignore` ignoring `.gradle/`, `build/`, `.cxx/`, and `local.properties`.
* Configured remote: `https://github.com/ScoRpiiTech/autoroid.git`.
* **`.github/workflows/release.yml`**: Added fully automated GitHub Actions release pipeline:
  - Triggers on push to `main` or version tag (`v*`).
  - Automatically installs NDK 27 and CMake 3.22.1 on Ubuntu runner.
  - Builds and signs release APK via `./gradlew assembleRelease`.
  - Publishes GitHub Release automatically with `app-release.apk` attached and generated release notes.
* Bumped app version to `1.2.0` (`versionCode = 2`).

---

## [v1.2.1] - Shizuku IPC Provider & Sticky Binder Connection Fix
* **Date:** 2026-09-17
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Root Cause Identification & Manifest Resolution
* **Missing `ShizukuProvider`**: In `dev.rikka.shizuku:provider:13.1.5`, the ContentProvider authority is app-specific and must be declared in the application's `AndroidManifest.xml`. Without `<provider android:name="rikka.shizuku.ShizukuProvider" android:authorities="${applicationId}.shizuku" ... />`, Shizuku Server was completely unable to deliver the IPC binder token to Autoroid.
* **Android 11+ Package Visibility**: Added `<queries><package android:name="moe.shizuku.privileged.api" /></queries>` to ensure `moe.shizuku.privileged.api` is visible on targetSdk 36.

### 2. Runtime Connection & Sticky Binder Lifecycle
* **`MainActivity.kt`**:
  - Registered `Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)` so the app immediately captures the binder token even if delivered before activity initialization.
  - Added `Shizuku.addBinderDeadListener(binderDeadListener)` to react dynamically if the Shizuku service is stopped.
  - Refactored `requestShizukuPermission()` to avoid throwing `IllegalStateException` when `pingBinder()` is false.
  - Added descriptive logging: informs the user if Shizuku service is not running and offers to open the Shizuku Manager app.

### 3. Elevated Command Execution (`ShizukuEngine.kt`)
* Refactored `execute()` from deprecated reflection calls to direct AIDL `moe.shizuku.server.IShizukuService.Stub.asInterface(Shizuku.getBinder()).newProcess(...)`.
* Streamed stdout and stderr via `ParcelFileDescriptor.AutoCloseInputStream` and retrieved exit codes with `remoteProcess.waitFor()`.
* Made `MainViewModel.log(msg)` public to surface connection diagnostics directly in the in-app terminal console.

### 4. Build & Distribution
* Bumped app version to `1.2.1` (`versionCode = 3`).
* Release APK signed with `autoroid-release.jks` and verified with Android SDK `apksigner` (Scheme v3).

---

## [v1.2.2] - Interactive Update Dialog & Header Version Display
* **Date:** 2026-09-17
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Interactive Update Status Modal
* **`UpdateStatusDialog.kt`**: Added dedicated Material 3 Cyber dark dialog replacing silent checks:
  - Version comparison grid: shows installed version (`v1.2.1`) side-by-side with online latest (`v1.2.1` or newer).
  - Status indicator:
    - 🟢 Up to Date with green verification checkmark.
    - 🚀 Update Available with action button "Install Update".
    - 🔄 Checking indicator while querying GitHub Releases API.
    - ⚠️ Informative network error & rate limit feedback.
  - Scrollable Release Notes section rendering formatted changelogs directly from GitHub.
  - Live in-dialog download progress bar and installation state spinners.

### 2. Persistent Header Version Badge
* **`HomeScreen.kt`**: Added a neon cyber badge (`v1.2.1`) to the top app bar next to the AUTOROID logo for immediate visibility.
* Wired the update icon button to launch `UpdateStatusDialog` and trigger `viewModel.checkForUpdates()`.

### 3. Engine Enhancements
* **`UpdateManager.kt`**: Exposed `currentVersion` property and improved HTTP error messages for rate-limits and empty release registries.
* **`MainViewModel.kt`**: Exposed `currentVersion` to the UI layer.

---

## [Security Audit & Hardening] - Public Open-Source Preparation
* **Date:** 2026-09-17
* **Status:** Verified (Keystore removed from Git tracking, Dynamic Secrets configured)

### 1. Keystore & Secret Isolation
* **Untracked `autoroid-release.jks`**: Removed keystore binary from Git tracking while preserving it locally.
* **Added Keystore Exclusions to `.gitignore`**: Excluded `*.jks`, `*.keystore`, `keystore.properties`, `*.p12`, `*.pem`, `*.key`, and `.env`.
* **Dynamic Signing Configuration (`app/build.gradle.kts`)**:
  - Dynamically loads signing properties from local gitignored `keystore.properties` or environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
  - Gracefully builds without error on public forks where the private keystore is absent.

### 2. GitHub Actions CI/CD Security (`.github/workflows/release.yml`)
* Configured automated release workflow to securely decode `RELEASE_KEYSTORE_BASE64` and inject passwords via GitHub Repository Secrets without ever publishing private keys.

### 3. Public Open-Source Security Guidelines
* Enforced strict repository security and privacy policies prohibiting any future commitment of secrets, keystores, tokens, or private credentials.

---

## [v1.2.3] - Version Synchronization & Smart Update Cache Pipeline
* **Date:** 2026-09-17
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Version Alignment & Loop Elimination
* **`app/build.gradle.kts`**: Bumped `versionCode = 4` and `versionName = "1.2.3"`.
* Fixed root cause of infinite update loop: previously, published GitHub release tags were ahead of internal Gradle `versionName`, causing the app to permanently detect newer versions on GitHub even after completing an update.

### 2. Smart Downloaded Package Verification
* **`UpdateInfo.kt`**: Added `isDownloaded: Boolean` flag to update status model.
* **`UpdateManager.kt`**:
  - Added `isApkDownloaded(apkFile, targetTag)` using Android `PackageManager.getPackageArchiveInfo()`.
  - Automatically verifies if cached APK already matches the latest remote tag.
  - Skips redundant 40MB downloads if the package is already downloaded in local cache.
  - Automatically cleans up obsolete cached APKs once the installed version matches the latest release.

### 3. Elevated Silent Self-Installation Across Root & Shizuku
* **`UpdateManager.kt`**:
  - **Root Path:** Directly executes `pm install -r -d <apkPath>` (UID 0).
  - **Shizuku Path:** Streams APK bytes to `/data/local/tmp/autoroid-update.apk` via remote shell stdin (`cat > /data/local/tmp/...`), executes elevated `pm install -r -d /data/local/tmp/...`, and cleans up the temporary file with 0 user prompts.
  - **PackageInstaller Fallback:** Safely flags file readability and triggers `FileProvider` with user notification if elevated privileges are unavailable.

### 4. UI Polish & Contextual Action Buttons
* **`UpdateStatusDialog.kt`**:
  - Dynamically updates action button between `"Install Update"` (when already cached) and `"Download & Install"` (when pending download).
  - Updates status badge to `"Release vX.X.X is downloaded and ready to install!"`.
### 5. Automated Storage & APK Purging
* **`UpdateManager.kt`**:
  - Added `purgeInstalledOrStaleApks()` executed at application startup to delete leftover update APKs once installed.
  - Added `purgeAllUpdateFiles()` immediately upon successful elevated installation before app restart, and whenever status reports up-to-date.
  - Ensures no 40MB+ update packages linger on device storage after installation.

---

## [v1.2.4] - Interactive UI Feedback, Feature Help Modals & Contextual Guidance
* **Date:** 2026-09-17
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Interactive Feature Information & Troubleshooting Dialogs
* **`FeatureInfoDialog.kt`**:
  - Created interactive Material 3 dialog system covering all subsystems: `ENGINE_PRIVILEGE`, `BANK_MODE`, `SIM_SWITCHER`, `WORKFLOWS`, and `COORDINATES`.
  - Explains technical mechanisms (e.g. how banking apps scan accessibility registries, direct modem telephony IPC, and pointer location overlay calibration).
  - Outlines exact prerequisites, elevation levels (Root vs Shizuku), and step-by-step usage instructions.
  - Integrated `(?)` info buttons on all cards in `HomeScreen.kt` and top app bar guide action.

### 2. Reactive UI Feedback Channel & Snackbars
* **`MainViewModel.kt`**:
  - Added reactive `_uiEvent = MutableSharedFlow<String>()` channel.
  - Emits immediate visual snackbar messages whenever actions are tapped:
    - Privilege warning when `PrivilegeLevel.NONE`: `"⚠️ Elevation Required: Connect Shizuku or grant Root..."`
    - Bank Mode activation/deactivation feedback with count of paused services.
    - SIM switcher line toggle confirmations and single-SIM warnings.
    - Pointer Location overlay toggle confirmations.
    - Real-time workflow execution start, step running, and completion feedback.
* **`HomeScreen.kt`**:
  - Wired `SnackbarHost(snackbarHostState)` with cyberpunk styled container and action buttons.

### 3. Smart Pre-requisite Badges & Disabled States
* **`WorkflowCard.kt`**:
  - Added `isPackageInstalled` check. Displays amber warning pill: `"⚠️ Target app \"[App Name]\" not installed"` so users immediately understand why pre-seeded workflows (e.g., Samsung Health) cannot launch.
* **`HomeScreen.kt`**:
  - `PrivilegeStatusCard`: Added prominent warning banner when unprivileged with direct instruction to launch Shizuku or grant Root.
  - `BankModeCard`: Added informational notice if 0 accessibility services are currently enabled.
  - `SimSwitcherCard`: Added single-SIM warning badge and dynamically disabled toggle button when `simSlots.size < 2` with clear label `"DUAL-SIM REQUIRED (ONLY 1 SIM FOUND)"`.

### 4. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 5` and `versionName = "1.2.4"`.

---

## [v1.2.5] - Bank Mode State Clarification & Dual-SIM eSIM Switching Architecture
* **Date:** 2026-09-17
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Bank Mode State Disambiguation & All Clear Mode
* **`AccessibilityController.kt`**:
  - Replaced ambiguous empty-string heuristic with explicit `is_bank_mode_engaged` state tracking.
  - Resolved infinite "Restore Services" loop: previously, devices naturally having 0 accessibility services enabled were incorrectly flagged as "Bank Mode Active", rendering a broken "Restore" button with nothing to restore.
  - Distinct state handling:
    - **All Clear (Safe for Banks):** When 0 services are running, displays "All Clear" badge with explanation that banking apps won't detect or block anything.
    - **Active Services:** Explicitly lists running components in amber and provides 1-tap "Activate Bank Mode (Pause X Services)".
    - **Protected (Engaged):** Displays the exact list of paused services and allows clean restoration.
* **`BankModeCard.kt` & `BankModeTileService.kt`**:
  - Dynamically updates labels, subtitles, and button states based on actual running vs paused services.

### 2. Dual-SIM Modem Switching with eSIM Support
* **`TelephonyController.kt`**:
  - Added full support for embedded subscriptions (**eSIM**, `info.isEmbedded`).
  - Added Shizuku direct Binder IPC via `ISub.setDefaultDataSubId(targetSubId)` and `ITelephony.setDataEnabledForReason(...)`, mirroring AOSP Settings behavior.
  - Integrated `org.lsposed.hiddenapibypass:hiddenapibypass:4.3` to access hidden telephony APIs across modern Android (Android 10 - 16).
  - Multi-tier active data SIM query: resolves active subscription via `SubscriptionManager.getDefaultDataSubscriptionId()`, `Settings.Global.multi_sim_data_call`, and elevated `dumpsys telephony.registry`.
  - Added elevated `dumpsys isub` block parser as fallback for devices where runtime permissions are not yet initialized.
* **`MainActivity.kt` & `MainViewModel.kt`**:
  - Added runtime `READ_PHONE_STATE` permission launcher.
  - Automatically executes `pm grant com.autoroid.app android.permission.READ_PHONE_STATE` via elevated shell whenever Root or Shizuku connects.
* **`SimSwitcherCard.kt` & `SimSwitchTileService.kt`**:
  - Badges each slot as `[eSIM]` or `[Physical SIM]` with carrier name and SubId.
  - Prominently displays green `[ACTIVE DATA]` badge on whichever SIM is currently powering mobile data.
  - Dynamic button updates: displays `"SWITCH DATA TO ESIM (CARRIER)"` or `"SWITCH DATA TO PHYSICAL SIM (CARRIER)"` with immediate visual confirmation.

### 3. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 6` and `versionName = "1.2.5"`.

---

## [v1.2.6] - Automated Scheduled SIM Data Switcher (Time Windows & Off-Peak Bundles)
* **Date:** 2026-09-17
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Automated SIM Schedule Engine & Midnight Windowing
* **`SimSchedule.kt`**:
  - Encapsulates scheduling state: `isEnabled`, `startHour`, `startMinute`, `endHour`, `endMinute`, `windowSubId`, and `defaultSubId`.
  - Circular 24-hour time window evaluator (`isTimeInWindow`): accurately computes whether current time falls within off-peak windows crossing midnight (e.g., `12:00 AM – 09:00 AM` or `11:00 PM – 07:00 AM`).
  - AM/PM time formatting utilities for clean 12-hour digital displays.
* **`SimScheduleRepository.kt`**:
  - Persistent state storage in private preferences (`autoroid_sim_schedule`).
  - Reactive `StateFlow<SimSchedule>` updates.
* **`SimScheduleManager.kt`**:
  - Core scheduling manager orchestrating background execution.
  - Automatically evaluates current time window on enable, boot, or alarm triggers and switches data line using elevated `TelephonyController.switchToSubId()`.
  - Dispatches non-intrusive Android status notifications informing the user when automated switches occur.
  - Computes exact millisecond timestamps and arms next transitions using `AlarmManager.setExactAndAllowWhileIdle()`.

### 2. Deep Doze & Reboot Recovery
* **`SimScheduleReceiver.kt`**:
  - Wakeful `BroadcastReceiver` invoked by `AlarmManager.RTC_WAKEUP`.
  - Acquires a short `PowerManager.PARTIAL_WAKE_LOCK` (10s max) via `goAsync()` to guarantee CPU execution through Android Deep Doze even when device is stationary and locked overnight.
* **`BootReceiver.kt`**:
  - Listens for `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.
  - Automatically restores alarms on boot and immediately applies the correct SIM for the current time.
* **`AndroidManifest.xml`**:
  - Declared `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, and `POST_NOTIFICATIONS` permissions.
  - Registered receivers for alarms and reboot recovery.

### 3. Modern Cyberpunk UI Components
* **`TimePickerDialog.kt`**:
  - Digital clock dialog with large cyberpunk digital display, hour/minute steppers, and AM/PM filter chips.
* **`SimScheduleCard.kt`**:
  - Integrated into the Dual-SIM dashboard.
  - Master ON/OFF toggle switch.
  - Live status banner indicating active window state, currently routed SIM, and upcoming switch time.
  - Start & End time picker chips with 1-tap dialogs.
  - Quick SIM selector chips for both within-window and daytime/default hours.
* **`FeatureInfoDialog.kt`**:
  - Added dedicated `SIM_SCHEDULE` guide explaining night data bundle automation, setup steps, and troubleshooting.
* **`HomeScreen.kt` & `MainViewModel.kt`**:
  - Wired reactive `simSchedule` flow, `updateSimSchedule()`, and `toggleSimSchedule()` with user snackbar feedback.

### 4. Release Changelog Publishing & In-App Resolution
* **`.github/workflows/release.yml`**:
  - Replaced GitHub's generic `generate_release_notes: true` (which produced only compare links when commits were made without PRs) with an automated parser step.
  - Extracts the exact version section directly from `CHANGELOG.md` and passes `body_path: release_notes.md` to `softprops/action-gh-release@v2`.
* **`app/build.gradle.kts`**:
  - Added `copyChangelogToAssets` task to automatically bundle `CHANGELOG.md` into the APK assets.
* **`UpdateManager.kt`**:
  - Added smart fallback: if GitHub release body contains only generic compare URLs or is blank, the app parses the exact version block from bundled assets or fetches raw `CHANGELOG.md` from the repository.
  - When the app is up to date, it displays the installed version's full feature changelog instead of an empty box.
* **`UpdateStatusDialog.kt`**:
  - Expanded changelog viewport height to `240.dp` with dynamic header labels.

### 5. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 7` and `versionName = "1.2.6"`.

---

## [v1.2.7] - Unified State-of-the-Art Dual-SIM & Modem Manager
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Unified Information Architecture
* **`DualSimManagerCard.kt`**:
  - Combined the manual line switcher and the automated time scheduler into a single, cohesive modem management station.
  - Eliminated UI redundancy and reduced screen clutter.
  - Seamless segmented capsule controller allowing instant switching between `[ ⚡ Instant Switch ]` and `[ ⏰ Auto Schedule ]` modes.

### 2. State-of-the-Art Cyberpunk Aesthetics & Microchip Cards
* **Interactive Hardware SIM Slots**:
  - Designed microchip cards representing Physical SIMs and eSIMs with antenna badges, carrier labels, and phone slot indicators.
  - **Pulsing Neon Beacon**: Integrated an animated glowing pulse (`rememberInfiniteTransition`) on the active cellular data line (`[ACTIVE DATA]`).
  - One-tap quick switching by tapping either hardware card directly.
* **Modern Gradient Borders & Glassmorphism**:
  - Outlined with subtle horizontal gradient strokes (`CyberCyan` $\to$ `CardBorder` $\to$ `NeonGreen`).
  - Clean digital time window capsules (`FROM (START)` and `TO (END)`) with glowing cyan/green accents.
  - Interactive SIM assignment pills for both within-window (off-peak) and default daytime lines.

### 3. Screen Optimization
* **`HomeScreen.kt`**:
  - Replaced separate `SimSwitcherCard` and `SimScheduleCard` with `DualSimManagerCard`.
  - Removed 220+ lines of legacy redundant card code from the main screen.

### 4. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 8` and `versionName = "1.2.7"`.

---

## [v1.2.8] - Native Pixel IMS & Carrier Config Patcher (Turbo IMS Killer)
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Built-in Pixel IMS & Carrier Config Engine
* **`ImsConfig.kt`**:
  - Full data model mapping carrier overrides to Android `CarrierConfigManager` keys.
  - Controls VoLTE (`carrier_volte_available_bool`, `enhanced_4g_lte_on_by_default_bool`), VoWiFi (`carrier_wfc_ims_available_bool`, `carrier_default_wfc_ims_enabled_bool`), 5G VoNR (`vonr_enabled_bool`, `carrier_vonr_available_bool`), Ut Interface (`carrier_supports_ss_over_ut_bool`), and Settings Visibility (`editable_enhanced_4g_lte_bool`, `editable_wfc_mode_bool`, `vonr_setting_visibility_bool`).
* **`ImsRepository.kt`**:
  - Persistent SharedPreferences storage (`autoroid_ims_carrier_config`) preserving customized IMS flags per SIM slot index.
* **`ImsController.kt`**:
  - Dual IPC execution engine:
    - **Primary Path (Shizuku Binder IPC):** Direct calls to `ICarrierConfigLoader.overrideConfig(subId, bundle, persistent = false)` via `SystemServiceHelper.getSystemService("carrier_config")`.
    - **Fallback Path (Elevated Shell IPC):** Elevated shell execution of `cmd phone cc set-value -s <slotId> <key> <value>` via `PrivilegeManager.executeElevated()`.
  - Supports applying overrides to individual SIM slots or batch-applying to all active SIMs at once.
  - Full carrier reset functionality via `cmd phone cc clear-values`.

### 2. Automatic Persistence Across Device Reboots
* **`BootReceiver.kt` & `AutoroidApp.kt`**:
  - Wired into `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED`.
  - Automatically queries active SIMs (Physical SIM + eSIM) and re-injects saved carrier overrides on system startup without requiring third-party tools like Turbo IMS or manual screen interaction.

### 3. Cyberpunk UI Integration
* **`ImsCarrierPatcherCard.kt`**:
  - Cyberpunk-styled card with glowing gradient borders and microchip SIM tabs (`Physical SIM` vs `eSIM`).
  - Live status banner indicating whether overrides are active and persisted.
  - Interactive switches with explanatory subtitles for VoLTE, VoWiFi, 5G VoNR, Ut Interface, and Settings Toggle Visibility.
  - Quick action buttons: `[ APPLY TO SIM ]`, `[ ⚡ APPLY TO ALL SIMS ]`, and `[ ↺ RESET ]`.
* **`FeatureInfoDialog.kt`**:
  - Added dedicated `CARRIER_IMS_PATCHER` user guide detailing Pixel carrier bypasses, regional whitelisting, and boot restoration.
* **`HomeScreen.kt` & `MainViewModel.kt`**:
  - Mounted `ImsCarrierPatcherCard` and wired reactive state management with snackbar feedback.

### 4. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 9` and `versionName = "1.2.8"`.

---

## [v1.2.9] - Android System Notification Engine for Releases & Updates
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. System Notification Shade Alerts
* **`UpdateManager.kt`**:
  - Registered high-importance notification channel: `autoroid_app_updates` ("App Updates").
  - Posts heads-up Android status bar notification whenever a newer release tag is detected on GitHub (`🚀 Autoroid vX.Y.Z Available`).
  - Includes expandable BigTextStyle preview with version comparison and changelog highlights.
  - Tapping the notification launches `MainActivity` with intent extra `EXTRA_OPEN_UPDATE = true`, automatically displaying the interactive update dialog.
  - Automatically cancels notification once the update is installed or when the app is confirmed up-to-date.

### 2. Background Startup & Boot Notification Trigger
* **`BootReceiver.kt`**:
  - Added background `updateManager.checkForUpdates()` call upon device boot (`ACTION_BOOT_COMPLETED`), alerting users to new updates even before opening the app.
* **`MainViewModel.kt` & `MainActivity.kt`**:
  - Added runtime `android.permission.POST_NOTIFICATIONS` permission launcher on Android 13+ (API 33+).
  - Automatically grants `POST_NOTIFICATIONS` via elevated Root/Shizuku execution (`pm grant com.autoroid.app android.permission.POST_NOTIFICATIONS`).

### 3. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 10` and `versionName = "1.2.9"`.

---

## [v1.2.10] - GitHub Release Pipeline Fix & Deterministic Update Engine
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Root Cause Fix: "Already Up To Date" & Missing Assets
* **Diagnosed Root Causes:**
  - Parallel GitHub Actions runners triggered on both `main` branch push and tag push raced to publish the same release, causing asset upload deadlocks and leaving releases with empty assets.
  - `UpdateManager.kt` previously calculated `hasUpdate = isNewer && downloadUrl.isNotBlank()`. When GitHub release assets were empty or still finalizing, `hasUpdate` evaluated to `false`, silently suppressing the update and reporting "Autoroid is up to date".
  - GitHub `/releases/latest` endpoint caching caused delayed visibility of newly published releases.
* **`UpdateManager.kt` Fixes:**
  - Switched endpoint to `/releases?per_page=5` with semver tag sorting to bypass CDN edge cache staleness.
  - Added deterministic fallback download URL (`https://github.com/$OWNER/$REPO/releases/download/$TAG/app-release.apk`) if asset metadata is delayed.
  - Decoupled `hasUpdate = isNewer` so newly available releases are always surfaced to the user.

### 2. CI/CD Release Pipeline Hardening
* **`.github/workflows/release.yml`**:
  - Removed `push: branches: [main]` so release jobs only trigger on explicit version tags (`v*`), completely eliminating concurrent runner collisions.
  - Migrated from third-party release action to native GitHub CLI (`gh release create` / `gh release upload --clobber`), preventing hanging and ensuring idempotent asset uploads.

### 3. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 11` and `versionName = "1.2.10"`.

---

## [v1.2.11] - Rate-Limit-Free Atom Feed Fallback for App Updates
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Bypass GitHub API Rate Limits
* **Problem:** GitHub REST API (`api.github.com`) restricts unauthenticated requests to 60 per hour per IP. On mobile cellular carrier networks (CGNAT) or shared Wi-Fi, multiple devices share a single IPv4 address, quickly triggering `403 rate limit exceeded`.
* **Solution:**
  - Implemented automatic fallback to GitHub's public Atom feed (`https://github.com/$OWNER/$REPO/releases.atom`).
  - Atom feeds are public web feeds served without rate limiting or authentication requirements.
  - Automatically parses latest release tag, publication timestamp, and HTML release notes via `android.text.Html`.
  - When `api.github.com` returns HTTP 403, 429, or network errors, `UpdateManager` transparently falls back to the Atom feed within milliseconds without failing or bothering the user.

### 2. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 12` and `versionName = "1.2.11"`.

---

## [v1.2.12] - Elevated BrokerInstrumentation Carrier Patcher (CVE-2025-48617 Bypass)
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Root Cause Fix: "Failed to Apply Override to SIM"
* **Identified Root Causes:**
  - In recent Android security updates (CVE-2025-48617 on Pixel 7 Pro, Android 14/15/16), Google patched the shell user from directly calling `CarrierConfigManager.overrideConfig()` or `ICarrierConfigLoader` with `SecurityException: overrideConfig cannot be invoked by shell`.
  - Android requires caller to be an **active instrumentation runner** started from the shell in order to invoke `startDelegateShellPermissionIdentity(Os.getuid(), null)`.
  - The `<instrumentation>` tag was not registered in `AndroidManifest.xml`.
  - `ImsController` previously fell back to non-existent shell command `cmd phone cc set-value`.
  - Command-line arguments passed via `am instrument` are received as `String` in `Bundle`, which caused `moder_subId` and boolean flags (`carrier_volte_available_bool`, etc.) to be misinterpreted unless parsed as typed Booleans and Integers.
* **Architecture Solution:**
  - **`AndroidManifest.xml`**: Registered `<instrumentation android:name=".feature.telephony.ims.BrokerInstrumentation" android:targetPackage="com.autoroid.app" />`.
  - **`BrokerInstrumentation.kt`**:
    - Robust string-to-type parsing for `moder_subId` and `PersistableBundle` boolean/int configuration flags.
    - Uses `IActivityManager.startDelegateShellPermissionIdentity(Os.getuid(), null)` within the active instrumentation context.
    - Calls `CarrierConfigManager.overrideConfig(subId, bundle, persistent = false)`.
    - Returns structured result bundle (`Activity.RESULT_OK` / `Activity.RESULT_CANCELED`).
  - **`ImsController.kt`**:
    - Rewrote `applyConfig` and `clearConfig` to execute `am instrument -w` via `PrivilegeManager` (Root UID 0 or Shizuku UID 2000 ADB shell).
    - Added error message extraction and diagnostics.

### 2. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 13` and `versionName = "1.2.12"`.

---

## [v1.2.13] - Luxury Adaptive App Icon & Material You Dynamic Theming
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. High-Class Luxury App Icon Redesign
* **Problem:** Replaced the legacy placeholder square icon (`#00E5FF` rectangle) with a state-of-the-art luxury design.
* **Design Philosophy:**
  - Futuristic titanium cybernetic shield emblem infused with glowing neon cyan (`#00E5FF`) and electric emerald (`#2EE59D`) microchip circuitry.
  - Dark obsidian glass depth background (`#080E17`).
  - Stylized cybernetic crest 'A' integrated with an elevated Android core processor.
  - Ultra-high resolution asset downsampled using Lanczos resampling for razor-sharp fidelity across all density buckets.

### 2. Full Android Adaptive Icon Architecture (`mipmap-anydpi-v26`)
* **Adaptive Icon Layers:**
  - `drawable/ic_launcher_background.xml`: Deep obsidian cyber grid background vector (`#080E17` / `#0A1424`).
  - `drawable-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.png`: High-resolution foreground layers scaled to 108dp canvas with safe zone margins to prevent clipping on circular and squircle launchers.
  - `drawable/ic_launcher_monochrome.xml`: Minimalist vector silhouette of the cyber shield, 'A' crest, and microchip core for Android 13+ (Pixel 7 Pro) Material You dynamic wallpaper color theming.
  - `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`: Updated to declare `<adaptive-icon>` referencing background, foreground, and monochrome assets.

### 3. Legacy Density Mipmaps
* Generated multi-density raster drawables for legacy launchers, Android Settings, notifications, and APK extractors:
  - `mipmap-mdpi/ic_launcher.png` & `ic_launcher_round.png` (48x48)
  - `mipmap-hdpi/ic_launcher.png` & `ic_launcher_round.png` (72x72)
  - `mipmap-xhdpi/ic_launcher.png` & `ic_launcher_round.png` (96x96)
  - `mipmap-xxhdpi/ic_launcher.png` & `ic_launcher_round.png` (144x144)
  - `mipmap-xxxhdpi/ic_launcher.png` & `ic_launcher_round.png` (192x192)

### 4. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 14` and `versionName = "1.2.13"`.

---

## [v1.2.14] - Fix App Launch Crash & In-Process Shell Permission Delegation
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Root Cause Analysis of Launch Crash
* **Problem:** Following the VoLTE/VoWiFi updates, users experienced immediate crashes on app launch ("not opening").
* **Identified Root Causes:**
  1. **`<instrumentation>` Manifest Conflict:** Declaring an `<instrumentation>` tag in `src/main/AndroidManifest.xml` targeting the app's own package (`com.autoroid.app`) caused Android's `ActivityThread` to treat the package as an instrumentation test target on app startup. Normal activity launches failed immediately with `SecurityException` / `Process crashed`.
  2. **Process Termination by `am instrument`:** Android's `ActivityManagerService` forcefully kills any existing process for `targetPackage` when `am instrument` is executed. Calling `am instrument` from `ImsController.onBoot()` resulted in Android killing Autoroid's process 2.5 seconds after launch.
  3. **Unprotected Startup Coroutine:** `applicationScope.launch` in `AutoroidApp.kt` lacked individual exception guards around subsystem initialization.

### 2. Implementation: In-Process Shell Permission Delegation
* **`AndroidManifest.xml`**: Completely removed the `<instrumentation>` tag.
* **Deleted `BrokerInstrumentation.kt`**: Eliminated the external instrumentation component.
* **`ImsController.kt`**:
  - Replaced `am instrument` with direct in-process Shell Permission Delegation via Shizuku:
    1. Acquires `IActivityManager` via `ShizukuBinderWrapper(getSystemService("activity"))`.
    2. Calls `startDelegateShellPermissionIdentity(Process.myUid(), null)` to grant the app's process shell-level identity.
    3. Directly calls `CarrierConfigManager.overrideConfig(subId, bundle, persistent = false)`.
    4. Safely releases delegation via `stopDelegateShellPermissionIdentity()`.
  - Added direct `ICarrierConfigLoader` Binder IPC fallback.
  - Added Root execution fallback (`pm grant com.autoroid.app android.permission.MODIFY_PHONE_STATE`).
  - Added safety wrappers around `onBoot()` so auto-restore never interrupts app startup.
* **`AutoroidApp.kt`**: Wrapped each subsystem's initialization in individual `try-catch` blocks to ensure fail-safe app launches.

### 3. Version Bump
* **`app/build.gradle.kts`**: Bumped `versionCode = 15` and `versionName = "1.2.14"`.

---

## [v1.2.15] - Full Turbo IMS / TensorIMS Privileged Instrumentation Architecture
* **Date:** 2026-09-18
* **Status:** Verified (Build Successful, Release APK Signed & Scheme v3 Verified)

### 1. Root Cause Analysis & Pixel Restriction Resolution
* **Bypassed CVE-2025-48617:** On modern Google Pixel devices (Android 14/15/16 with the October 2025 security update), Android's `CarrierConfigLoader` blocks `Process.SHELL_UID` (UID 2000, Shizuku) from invoking `overrideConfig()` with `SecurityException: overrideConfig cannot be invoked by shell`.
* **Resolved Process Termination:** Previous attempts invoking `am instrument` from the shell killed Autoroid because CLI tools start instrumentation with flag `0` (restarting target process). In contrast, `IActivityManager.startInstrumentation()` with `flags = 8` (`INSTR_FLAG_NO_RESTART`) launches the instrumentation runner inside the already-running app process without killing or restarting it!

### 2. Compile-Time Framework Stub Module (`:stub`)
* Introduced a dedicated `:stub` library module providing compile-only Android framework classes (`compileOnly(project(":stub"))` with 0 APK footprint):
  - `stub/src/main/aidl/android/app/IInstrumentationWatcher.aidl`: Status and completion callbacks from `ActivityManagerService`.
  - `stub/src/main/java/android/app/IActivityManager.java`: `startInstrumentation` and shell permission delegation methods.
  - `stub/src/main/java/android/app/UiAutomationConnection.java`: UiAutomation connection stub.
  - `stub/src/main/aidl/com/android/internal/telephony/ITelephony.aidl`: Telephony reset and registration checks.

### 3. Privileged Instrumentation Engine
* **`ImsModifier.kt`**:
  - Registered in `AndroidManifest.xml` targeting `${applicationId}`.
  - Launched dynamically via `am.startInstrumentation(..., flags = 8, ...)`.
  - Executes `startDelegateShellPermissionIdentity(Os.getuid(), null)` (allowed because `isCallerInstrumentation() == true`).
  - Calls `CarrierConfigManager.overrideConfig(subId, bundle, persistent = false)`.
  - Performs persistent hardware VoLTE provisioning directly on modem NVRAM:
    - `ProvisioningManager.setProvisioningIntValue(KEY_VOIMS_OPT_IN_STATUS, 1)`
    - `ImsMmTelManager.setAdvancedCallingSettingEnabled(true)`
    - `SubscriptionManager.setSubscriptionProperty("ENHANCED_4G_MODE_ENABLED", "1")`
    - `SubscriptionManager.setSubscriptionProperty("VOIMS_OPT_IN_STATUS", "1")`
  - Safely releases delegation via `stopDelegateShellPermissionIdentityCompat()`.
* **`ImsResetter.kt`**: Resets carrier configurations and resets IMS via `ITelephony.resetIms()`.
* **`ImsCapabilityReader.kt`**: Live inspection of VoLTE, VoWiFi, VoNR, VT, and 5G NSA/SA availability.

### 4. Build & Distribution
* Bumped `versionCode = 16` and `versionName = "1.2.15"`.
* Verified with `apksigner` (APK Signature Scheme v3).
