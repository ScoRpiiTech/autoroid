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
3. **Dynamic Bank Mode**: Snapshot and restore system accessibility registries on-the-fly to bypass aggressive anti-automation detection in modern banking and enterprise apps.
4. **Any-App Workflow Engine**: Multi-step macro engine supporting Smart UI Clicks (text/content-desc via `uiautomator`), coordinate tapping, swipe gestures, app launch intents, delays, and elevated shell commands.
5. **Dual-SIM Mobile Data Switcher**: Instant data line failover via `ITelephony` IPC with Quick Settings shade tiles.
6. **In-App Self-Update System**: Integrated with GitHub Releases for 1-click elevated silent self-installation.

---

## 🛠️ Architecture & Tech Stack

| Component | Specification |
| :--- | :--- |
| **Target OS** | Android 10+ (`minSdk = 29`, `targetSdk = 36`, `compileSdk = 36`) |
| **Languages** | Kotlin 2.0.20, C++17, Java 17/19 |
| **UI Framework** | Jetpack Compose Material 3 (Cyber Dark theme) |
| **Native Toolchain** | Android NDK `27.0.12077973`, CMake `3.22.1` |
| **Privilege Backends** | `rikka.shizuku:api:13.1.5`, Native su (`KernelSU`, `APatch`, `Magisk`) |

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

### 4. Any-App Workflow Engine
Create, edit, reorder, and execute dynamic multi-step automations:
* `LaunchApp`: Elevated activity start or launcher intent.
* `SmartClickText`: Fast XML UI dump parser that locates elements by text or content-description and injects center taps.
* `TapCoordinate`: Direct coordinate touch injection (`input tap X Y`).
* `PointerLocation`: Built-in real-time $(X, Y)$ screen coordinate inspector overlay.
* `Delay`, `Swipe`, and `ShellCommand`: Flexible macro building blocks.

### 5. In-App Self-Update
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
