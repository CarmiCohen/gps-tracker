# Project Issues & Hardening Tracking (Sep.02.41)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 19 |
| **Validation Tasks** | 🟢 Validated | 222 |
| **Resolved (Total)** | 🟢 Progress | 818 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #898: HUD Telemetry Stalled in Tracker Mode**. `MainViewModel` is missing the observation/collection logic for local location and health flows. Telemetry remains at baseline (`--`) despite active service sampling (R3.1).

---

## 🔴 Open Issues
*   *Ongoing technical debt and minor hardening tasks.*

---

## 🟢 Recently Resolved Issues (Sep.02.41)
*   **Issue #897 RESOLVED: Sensor Sensitivity Sliders Disconnected**. Propagated UI sensitivity settings (Vibration/Tilt) to the engine. Mapped 0.0-1.0 range to dynamic thresholds in `SentinelValidator` to replace hardcoded constants (R2.3). (Sep.02.41).
*   **Issue #896 RESOLVED: Battery Optimization Navigation Failure**. Hardened `launchBatteryExemptionSetting` with multi-tier fallback (Request -> Settings -> App Info) and standardized `Uri.fromParts` for all permission intents to ensure Samsung A15/Android 15 compatibility (R896). (Sep.02.40).
*   **Issue #895 RESOLVED: Android 15 16KB Page Size Compatibility**. Native libraries aligned for Android 15+ 16KB devices via AGP 8.5.1 upgrade, `-Wl,-z,max-page-size=16384` linker flags, and `useLegacyPackaging = false` (R895). (Sep.02.27).
*   **Issue #893 RESOLVED: Native Resource Disposal Leak (BaseEventQueue)**. remediated `BaseEventQueue.dispose` failures by ensuring all `ManagedNetworkCallback` instances are registered and unregistered on the `MainLooper` (R893). (Sep.01.27).
*   **Issue #894 RESOLVED: logcat Overhead (getPackageName Spam)**. Implemented `ContextShadow` delegate (R894) to provide cached package name to system services, eliminating IPC-based diagnostic log spam. (Sep.01.26).
*   **Issue #892 RESOLVED: WorkManager Initialization Failure (R892)**. Resolved `IllegalStateException` in `BootReceiver` by implementing manual initialization in `GpsApplication.onCreate()`. (Verified Sep.01.25).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.41)*
*Simplification Ideas: 237*
