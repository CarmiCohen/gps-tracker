# Project Issues & Hardening Tracking (Sep.10.00)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **SRV Status Inconsistency (#941)**: In version Sep.08.13, peer status inconsistency observed where one device shows SRV in RED (Disconnected/Error) and the other device shows SRV in GREEN (Connected). This indicates a signaling synchronization gap during role transitions or recovery.

## 🟢 Recently Resolved Issues (Sep.10.00)
*   **Legacy Field Cleanup Hardening RESOLVED (#284)**: Remediated `NoSuchMethodError` crashes in `TrackerScreen.kt` and `ViewerScreen.kt` by migrating all direct `LocationUpdate` field accesses to the partitioned state structure (`.kinetic`, `.atmospheric`, `.integrity`).
*   **Peer Status LED Verification RESOLVED (#943)**: Finalized and verified the behavior of Peer Role LEDs (VWR/TRK) in `StatusBar` and `Dashboard`. 
    *   **Logic Enforcement (R972)**: Verified that peer indicators correctly default to **RED** (Rose500) in single-device isolation tests when no remote telemetry is detected.
    *   **Visual Consistency**: Confirmed that the `VWR` badge in Tracker mode and `TRK` badge in Viewer mode correctly reflect the `isTelemetryFresh` state.

## 🟢 Recently Resolved Issues (Sep.09.16)
*   **Identity Color Role Confusion RESOLVED (#942)**: Remediated role-color confusion in icons, StatusBar badges, and Dashboard metrics.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 304 (Rules: 54, IDs: 250), Resolved: 973, Open: 1, Testing: 100% (Sub-items: 50), Ideas: 5, QA: 268]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.10.00)*
