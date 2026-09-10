# Project Issues & Hardening Tracking (Sep.09.16)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **SRV Status Inconsistency (#941)**: In version Sep.08.13, peer status inconsistency observed where one device shows SRV in RED (Disconnected/Error) and the other device shows SRV in GREEN (Connected). This indicates a signaling synchronization gap during role transitions or recovery.

## 🟢 Recently Resolved Issues (Sep.09.16)
*   **Identity Color Role Confusion RESOLVED (#942)**:
    *   **Map Icons**: Fixed `MapOverlayManager.kt` where both icons used Cyan. Tracker now correctly uses `BrandJd` Green.
    *   **StatusBar**: Remediated role-color confusion in `SharedUiComponents.kt`. Peer badges (TRK/VWR) and local status indicators now strictly follow R799 color identity rules.
    *   **Dashboard**: Updated `OverlayComponents.kt` to ensure metric labels and values use identity colors (Tracker=Green, Viewer=Cyan) regardless of active app mode.

## 🟢 Recently Resolved Issues (Sep.09.15)
*   **Watchdog Precision Audit RESOLVED (R-ID 302)**: Remediated cumulative scheduling drift in `SystemMonitor.kt` by implementing grid-aligned watchdog pulses.
*   **A15 Hysteresis Audit RESOLVED (R-ID 274)**: Verified GNSS jitter suppression on Samsung A15.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 304 (Rules: 54, IDs: 250), Resolved: 971, Open: 1, Testing: 100% (Sub-items: 50), Ideas: 5, QA: 268]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.09.16)*
