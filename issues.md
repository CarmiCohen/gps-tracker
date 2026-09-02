# Project Issues & Hardening Tracking (Sep.02.42)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 18 |
| **Validation Tasks** | 🟢 Validated | 222 |
| **Resolved (Total)** | 🟢 Progress | 819 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No high-priority risks identified in this session.*

---

## 🔴 Open Issues
*   *Ongoing technical debt and minor hardening tasks.*

---

## 🟢 Recently Resolved Issues (Sep.02.42)
*   **Issue #898 RESOLVED: HUD Telemetry Stalled in Tracker Mode**. Integrated observation of `localLocation` and `trackerLocation` flows in `MainViewModel`. Telemetry and health updates are now correctly mapped to UI states, ensuring HUD and Dashboard values are live during active tracking (R3.1). (Sep.02.42).
*   **Issue #897 RESOLVED: Sensor Sensitivity Sliders Disconnected**. Propagated UI sensitivity settings (Vibration/Tilt) to the engine. Mapped 0.0-1.0 range to dynamic thresholds in `SentinelValidator` to replace hardcoded constants (R2.3). (Sep.02.41).
*   **Issue #896 RESOLVED: Battery Optimization Navigation Failure**. Hardened `launchBatteryExemptionSetting` with multi-tier fallback (Request -> Settings -> App Info) and standardized `Uri.fromParts` for all permission intents to ensure Samsung A15/Android 15 compatibility (R896). (Sep.02.40).
*   **Issue #895 RESOLVED: Android 15 16KB Page Size Compatibility**. Native libraries aligned for Android 15+ 16KB devices via AGP 8.5.1 upgrade, `-Wl,-z,max-page-size=16384` linker flags, and `useLegacyPackaging = false` (R895). (Sep.02.27).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.42)*
*Simplification Ideas: 237*
