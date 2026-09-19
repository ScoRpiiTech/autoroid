<div align="center">

  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="Autoroid Logo" />

  # Autoroid
  
  ### *The Elevated Power Suite & Automation Engine for Modern Android*
  **Targeting Android 15, 16 & 17 Preview (`targetSdk = 36`, `compileSdk = 36`)**

  <p align="center">
    <a href="https://github.com/ScoRpiiTech/autoroid/releases/latest">
      <img src="https://img.shields.io/github/v/release/ScoRpiiTech/autoroid?style=for-the-badge&color=00E5FF&label=Latest%20Release" alt="Latest Release" />
    </a>
    <a href="https://github.com/ScoRpiiTech/autoroid/releases">
      <img src="https://img.shields.io/github/downloads/ScoRpiiTech/autoroid/total?style=for-the-badge&color=2EE59D&label=Downloads" alt="Total Downloads" />
    </a>
    <a href="https://github.com/ScoRpiiTech/autoroid/blob/main/LICENSE">
      <img src="https://img.shields.io/badge/License-Apache%202.0-F59E0B?style=for-the-badge" alt="License" />
    </a>
    <br/>
    <a href="#">
      <img src="https://img.shields.io/badge/Android-10%20to%2017-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Versions" />
    </a>
    <a href="#">
      <img src="https://img.shields.io/badge/Privilege-Root%20%7C%20Shizuku-EF4444?style=for-the-badge" alt="Privilege Modes" />
    </a>
    <a href="#">
      <img src="https://img.shields.io/badge/Core-NDK%2027%20Native%20C%2B%2B17-6366F1?style=for-the-badge" alt="NDK C++" />
    </a>
  </p>

  <p align="center">
    <b>No brittle accessibility lag. No PC required. No battery-draining VPN tunnels.</b><br/>
    Direct Linux kernel and elevated Android IPC power at your fingertips.
  </p>

  <p align="center">
    <a href="https://github.com/ScoRpiiTech/autoroid/releases/latest">
      <img src="https://img.shields.io/badge/📥%20DOWNLOAD%20LATEST%20APK-v1.3.0%20(Release)-00E5FF?style=for-the-badge&labelColor=0D1117" height="38" alt="Download APK" />
    </a>
  </p>

</div>

---

## ⚡ Why Autoroid?

Traditional Android automation tools and tweak apps rely on outdated hacks: slow Accessibility overlays that lag or get blocked by banking apps, simulated VPN tunnels that consume 15–20% of your battery just to block internet, or separate root-only utility apps scattered across your device.

**Autoroid unites all critical power-user capabilities into one unified, cyber-glassmorphic cockpit.**

| Feature Comparison | **Autoroid** 🚀 | Tasker / MacroDroid | Turbo IMS / TensorIMS | NetGuard / VPN Firewalls |
| :--- | :---: | :---: | :---: | :---: |
| **Privilege Engine** | **Dual Root (UID 0) + Shizuku (UID 2000)** | Accessibility / Root | Shizuku / Root | VpnService |
| **Execution Speed** | **Sub-millisecond Native C++17** | Interpreted Script | Java/Kotlin | User-space Proxy |
| **Google Pixel IMS & VoLTE Unlocking** | ✅ **Full 16-Flag Engine** | ❌ None | ✅ IMS Only | ❌ None |
| **Per-App Internet Blocker (Firewall)** | ✅ **VPN-Free (Kernel `iptables`)** | ❌ None | ❌ None | ⚠️ Battery-heavy local VPN |
| **Bank App Detection Bypass (Bank Mode)** | ✅ **1-Tap Stealth Restore** | ❌ Gets detected | ❌ None | ❌ None |
| **Autonomous Hardware Event Triggers** | ✅ **Charger, Lock, Battery, Wi-Fi** | ⚠️ Heavy battery draw | ❌ None | ❌ None |
| **Background App Freezer & Auto-Sleep** | ✅ **`pm suspend` + `am force-stop`** | ⚠️ Partial | ❌ None | ❌ None |
| **Off-Peak Night SIM Data Switcher** | ✅ **Exact Alarms + Midnight Rollover** | ⚠️ Complex setup | ❌ None | ❌ None |
| **Shizuku Wireless ADB Auto-Starter** | ✅ **1-Tap Built-In Daemon Starter** | ❌ None | ❌ None | ❌ None |
| **License & Pricing** | **100% Free & Open Source (Apache 2.0)** | Paid / Freemium | Free / Open Source | Freemium / In-App |

---

## 📱 Navigation Spaces & Feature Showcase

Autoroid features an edge-to-edge **Cyber-Glassmorphic Multi-Tab UI** engineered for speed and clarity:

```
┌─────────────────────────────────────────────────────────────┐
│  ⚡ IMS & TELEPHONY  │  🛡️ BANK SHIELD  │  🤖 AUTOMATIONS  │  💻 CONSOLE  │
└─────────────────────────────────────────────────────────────┘
```

### 1. ⚡ IMS & Telephony: Carrier & Modem Patcher
> **Unlock VoLTE, VoWiFi, and 5G VoNR on Google Pixel & modern Android devices worldwide.**

Google restricts carrier features on Pixel devices in unsupported countries (Pakistan, India, Latin America, Southeast Asia, etc.). Android's `CarrierConfigLoader` also blocks shell processes from calling `overrideConfig()` via CVE-2025-48617.

Autoroid bypasses this entirely using **Two-Tier Privileged Instrumentation**:
* **16-Feature Complete Matrix**:
  * 📞 **VoLTE**: Voice over LTE high-definition audio.
  * 📶 **VoWiFi & VoWiFi Roaming**: Native Wi-Fi Calling at home and abroad.
  * 🚀 **VoNR (5G Voice)**: Native voice calling over standalone 5G (SA).
  * 📹 **Video Telephony (VT)**: Carrier ViLTE calling without third-party apps.
  * 🔄 **Cross-SIM Calling**: Seamless Wi-Fi Calling routed through your secondary SIM's cellular data.
  * 📡 **5G SA / NSA Modem Announcement**: Explicit baseband modem broadcast (`[1, 2]`).
  * ⚡ **5G+ / Ultra Wideband Icon**: Enforce status bar `5G+` / `5G_UW` on C-Band and mmWave.
  * 🎯 **5G Signal Boundary Calibration**: Fine-tune SS-RSRP threshold brackets (`[-128, -98 dBm]`).
  * 🔤 **Status Bar Tweaks**: Display "4G" instead of "LTE", and hide the "LTE+" carrier aggregation badge.
  * 🏷️ **Carrier Identity Overrides**: Custom carrier display name and custom IMS SIP User-Agent string.
* **Persistent Modem NVRAM Provisioning**: Writes persistent hardware provisioning directly to device modem NVRAM via `ProvisioningManager` and `ImsMmTelManager`.
* **Automatic Boot Restoration**: Built-in `BootReceiver` automatically reapplies your overrides whenever the device restarts—no external apps needed!

---

### 2. 🛡️ Bank Shield & Device Privacy
> **Zero-compromise banking security, kernel-level firewall, and instant app freezing.**

* **🏦 Bank Mode (Accessibility Cloak)**:
  * *The Problem*: Banking, enterprise, and streaming apps detect running Accessibility Services and refuse to launch or block logins.
  * *The Solution*: 1 tap snapshots all running accessibility services to secure storage, clears `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, and sets `accessibility_enabled 0`. When you're done banking, 1 tap restores all your services with exact configurations intact!
  * Includes a dedicated **Quick Settings Notification Shade Tile**.
* **🌐 NetCut (VPN-Free Per-App Firewall)**:
  * Cut off internet access for any game, social media app, or background tracker.
  * **No local VPN**: Saves battery and leaves your VPN slot free for real VPNs (WireGuard, Tailscale, Cloudflare WARP).
  * **Root Mode**: Direct Linux kernel packet dropping via `iptables -I OUTPUT -m owner --uid-owner <UID> -j DROP` and `ip6tables`.
  * **Shizuku Mode**: Strict policy isolation via `cmd netpolicy add restrict-background-blacklist <UID>` and `appops set <pkg> RUN_IN_BACKGROUND ignore`.
* **❄️ Background App Freezer & Deep Sleep**:
  * Silences resource-heavy apps completely using elevated package suspension (`pm suspend`) and instant process termination (`am force-stop`).
  * **Auto-Freeze on Screen Off**: Set it once, and Autoroid automatically puts battery-draining apps into deep sleep the moment your phone screen locks.

---

### 3. 🤖 Automations: Dynamic Workflows & Smart Triggers
> **Build powerful app macros that run manually or trigger autonomously on hardware events.**

* **Autonomous Hardware & System Event Triggers**:
  * ⚡ **Charger Connected / 🔌 Disconnected**: Trigger bedtime or car dock routines instantly.
  * 🔓 **Screen Unlocked / 🔒 Screen Off**: Execute actions when waking up or locking your device.
  * 🪫 **Battery Low ($\le$ 20%)**: Automate extreme power-saving sequences.
  * 🛜 **Wi-Fi Connected**: Fire routines when arriving home, at the office, or on specific SSIDs.
* **Polymorphic Macro Steps**:
  * 🚀 **`LaunchApp`**: Fast activity starts or package launcher intents.
  * 🔍 **`SmartClickText`**: High-speed XML hierarchy inspection via `uiautomator`. Finds elements by visible text or content-description, calculates exact screen midpoints, and injects taps.
  * 🎯 **`TapCoordinate`**: Pixel-accurate screen coordinate touch injection (`input tap X Y`).
  * ⏱️ **`Delay`**, 👆 **`Swipe`**, and 💻 **`ShellCommand`**: Combine into flexible, powerful workflows.
* **📍 Screen Pointer Coordinates Inspector**:
  * 1-tap overlay showing live touch $(X, Y)$ coordinates directly on your status bar for instant macro calibration.

---

### 4. 💻 Console: Elevated Terminal & Shizuku Starter
> **A dedicated power terminal and local Wireless ADB daemon starter.**

* **⚡ Shizuku Wireless ADB Auto-Starter**:
  * Automatically detects active Wireless Debugging ports (`adb.tls.port` / `service.adb.tls.port`).
  * Launches the Shizuku server daemon in 1 tap without requiring a computer or second device.
* **💻 Elevated Shell Terminal**:
  * Run arbitrary shell commands with real-time streaming output.
  * Quick-access chips for device diagnostics: `id`, `getprop`, `dumpsys telephony.registry`, `ip link`, and `iptables -L`.

---

### 5. 🔄 Automated Scheduled SIM Data Switcher
> **Effortlessly manage off-peak data packages across physical SIMs and eSIMs.**

* **Circular 24-Hour Scheduling**: Handles arbitrary midnight rollovers (e.g. switch to SIM 2 for unlimited night data between 11:00 PM and 7:00 AM, or 12:00 AM and 9:00 AM).
* **Exact Alarms (`AlarmManager`)**: Survives Android Deep Doze using `setExactAndAllowWhileIdle()` and wakeful alarm receivers.
* **Survives Device Reboots**: Automatically re-registers exact alarms on `BOOT_COMPLETED`.
* Includes a **Quick Settings Tile** for 1-tap manual SIM data switching from anywhere.

---

### 6. 🚀 In-App Self-Update System
* Automatically checks GitHub Releases for new updates.
* **Silent 1-Click Install**: Installs updates silently via Root (`pm install -r -d`) or Shizuku streaming (`cat > /data/local/tmp/...`), and automatically restarts the app without prompting you with system installer dialogs.
* Includes background notifications for new releases.

---

## 📥 Installation & Quick Start

### 1. Download & Install
Grab the latest release APK directly from GitHub:
👉 **[Download Latest Release APK](https://github.com/ScoRpiiTech/autoroid/releases/latest)**

### 2. Choose Your Elevation Mode

#### Option A: With Shizuku (No Root Required)
1. Install and open [Shizuku](https://shizuku.moe/).
2. Enable **Wireless Debugging** in Android Developer Options.
3. Start Shizuku (or use Autoroid's built-in **Shizuku Starter** in the Console tab!).
4. Open Autoroid and tap **Authorize** when prompted.

#### Option B: With Root (Magisk / KernelSU / APatch)
1. Open Autoroid.
2. Grant Superuser access when prompted.
3. Enjoy 100% full kernel-level and system privileges instantly.

---

## 🛠️ Building from Source

### Prerequisites
* **JDK 17 or JDK 19**
* **Android SDK** (`compileSdk = 36`, `targetSdk = 36`, `build-tools 36.0.0`)
* **Android NDK** `27.0.12077973` and **CMake** `3.22.1`

### Build Commands
```bash
# Clone the repository
git clone https://github.com/ScoRpiiTech/autoroid.git
cd autoroid

# Set JDK environment (example for macOS)
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-19.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Build Debug APK
./gradlew assembleDebug

# Build Signed Release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Release Signing
Release builds can be signed locally by creating a `keystore.properties` file in the project root:
```properties
storeFile=../your-keystore.jks
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```
*(Or by providing environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)*

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><b>Does Autoroid require Root?</b></summary>
<p>No! Autoroid is fully functional with <b>Shizuku</b> (which only requires Wireless ADB). Carrier & IMS patching, Bank Mode, workflow macros, scheduled SIM switching, app freezing, and Shizuku starting all work over Shizuku without Root. Having Root simply unlocks additional Linux kernel capabilities like hardware <code>iptables</code> filtering.</p>
</details>

<details>
<summary><b>Will the IMS Patcher work on my Google Pixel?</b></summary>
<p>Yes! Autoroid features 100% parity with TensorIMS / Turbo IMS. It uses privileged instrumentation to bypass CVE-2025-48617 on Android 14, 15, and 16, restoring VoLTE, VoWiFi, and 5G VoNR in unsupported regions and on unsupported carriers.</p>
</details>

<details>
<summary><b>Does NetCut drain my battery like VPN apps?</b></summary>
<p>Not at all! Unlike NetGuard or other VPN-based firewalls that route all your internet traffic through a local loopback VPN process, Autoroid's NetCut uses kernel-level <code>iptables</code> (Root) or system <code>netpolicy</code> (Shizuku). Outbound packets from blocked apps are dropped immediately at the OS layer, resulting in <b>zero</b> battery or performance penalty.</p>
</details>

<details>
<summary><b>Is Autoroid safe to use with banking apps?</b></summary>
<p>Yes. Bank Mode was specifically engineered to bypass aggressive banking anti-fraud scanners. By temporarily wiping the active accessibility services registry, banking apps perceive your device as clean and unautomated. Once your banking session is over, 1 tap restores your services instantly.</p>
</details>

---

## 📄 License

Autoroid is distributed as free and open-source software under the **Apache License 2.0**.  
See the [LICENSE](LICENSE) file for more details.

---

<div align="center">
  <sub>Engineered with ⚡ for Android power users, tinkerers, and developers worldwide.</sub><br/>
  <b><a href="https://github.com/ScoRpiiTech/autoroid">⭐ Star Autoroid on GitHub</a></b>
</div>
