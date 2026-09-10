# Project Issues & Hardening Tracking (Sep.10.05)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority open issues.*

## 🟢 Recently Resolved Issues (Sep.10.05)
*   **SRV Status Inconsistency RESOLVED (#941)**: Remediated peer status inconsistency (one device RED, one GREEN) during role transitions. 
    *   **Root-Cause Remediation**: Hardened `TelemetryRepository.clear()` and `ConnectivitySuite.stop()` to explicitly reset `isRelayConnected` and `lastRtt` flows, ensuring immediate UI feedback when signaling is terminated (R941).
*   **Legacy Field Cleanup Hardening RESOLVED (#284)**: Remediated `NoSuchMethodError` crashes in `TrackerScreen.kt` and `ViewerScreen.kt` by migrating all direct `LocationUpdate` field accesses to the partitioned state structure (`.kinetic`, `.atmospheric`, `.integrity`).
*   **Peer Status LED Verification RESOLVED (#943)**: Finalized and verified the behavior of Peer Role LEDs (VWR/TRK) in `StatusBar` and `Dashboard`. 
    *   **Logic Enforcement (R972)**: Verified that peer indicators correctly default to **RED** (Rose500) in single-device isolation tests when no remote telemetry is detected.

## 🟢 Recently Resolved Issues (Sep.09.16)
*   **Identity Color Role Confusion RESOLVED (#942)**: Remediated role-color confusion in icons, StatusBar badges, and Dashboard metrics.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 305 (Rules: 55, IDs: 250), Resolved: 974, Open: 0, Testing: 100% (Sub-items: 50), Ideas: 5, QA: 269]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.10.05)*
