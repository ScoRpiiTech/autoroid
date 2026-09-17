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
* **`UpdateBannerCard.kt`**:
  - Shows `"Downloaded • Ready to install"` and `"SELF-INSTALL UPDATE"` button when package is present locally.




