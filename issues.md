# Project Issues & Hardening Tracking (Sep.02.43)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 17 |
| **Validation Tasks** | 🟢 Validated | 222 |
| **Resolved (Total)** | 🟢 Progress | 820 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No high-priority risks identified in this session.*

---

## 🔴 Open Issues
*   *Ongoing technical debt and minor hardening tasks.*

---

## 🟢 Recently Resolved Issues (Sep.02.43)
*   **Issue #894 RESOLVED: ContextShadow Coverage Expansion**. Expanded `ContextShadow` delegate usage to `SystemStatusProvider`, `SystemMonitor`, `AppNotificationManager`, `AudioSynthesizer`, and `Utils`. This ensures all high-frequency system service lookups (Power, Alarms, Notifications, Audio) are optimized to silence `getPackageName` IPC diagnostic logs on Samsung A15/Android 15 (R1.14). (Sep.02.43).
*   **Issue #898 RESOLVED: HUD Telemetry Stalled in Tracker Mode**. Integrated observation of `localLocation` and `trackerLocation` flows in `MainViewModel`. Telemetry and health updates are now correctly mapped to UI states, ensuring HUD and Dashboard values are live during active tracking (R3.1). (Sep.02.42).
*   **Issue #897 RESOLVED: Sensor Sensitivity Sliders Disconnected**. Propagated UI sensitivity settings (Vibration/Tilt) to the engine. Mapped 0.0-1.0 range to dynamic thresholds in `SentinelValidator` to replace hardcoded constants (R2.3). (Sep.02.41).
*   **Issue #896 RESOLVED: Battery Optimization Navigation Failure**. Hardened `launchBatteryExemptionSetting` with multi-tier fallback (Request -> Settings -> App Info) and standardized `Uri.fromParts` for all permission intents to ensure Samsung A15/Android 15 compatibility (R896). (Sep.02.40).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.43)*
*Simplification Ideas: 240*
