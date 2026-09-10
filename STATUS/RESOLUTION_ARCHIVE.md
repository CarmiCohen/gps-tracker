# Resolution Archive (Sep.10.00)

## 🟢 Resolved Issues (Sep.10.00)
*   **Legacy Field Cleanup Hardening RESOLVED (#284)**: Remediated `NoSuchMethodError` crashes in `TrackerScreen.kt` and `ViewerScreen.kt` by migrating all direct `LocationUpdate` field accesses to the partitioned state structure (`.kinetic`, `.atmospheric`, `.integrity`).
*   **Peer Status LED Verification RESOLVED (#943)**: Finalized and verified the behavior of Peer Role LEDs (VWR/TRK) in `StatusBar` and `Dashboard`. 
    *   **Logic Enforcement (R972)**: Verified that peer indicators correctly default to **RED** (Rose500) in single-device isolation tests when no remote telemetry is detected.
    *   **Visual Consistency**: Confirmed that the `VWR` badge in Tracker mode and `TRK` badge in Viewer mode correctly reflect the `isTelemetryFresh` state.

## 🟢 Resolved Issues (Sep.09.16)
*   **Identity Color Role Confusion RESOLVED (#942)**: 
    *   **Map Icons**: Remediated `MapOverlayManager.kt` where both icons used Cyan. Fixed `createTrackerBitmap` to use `BrandJd` Green with `Style.FILL` for the inner circle.
    *   **HUD StatusBar**: Fixed `SharedUiComponents.kt` to ensure peer badges (TRK/VWR) use role-appropriate colors (`BrandJd`/`ViewerCyan`). Local status indicators and progress bars now also follow role identity.
    *   **Telemetry Dashboard**: Refined `OverlayComponents.kt` to enforce identity colors for metrics (Tracker=Green, Viewer=Cyan) regardless of active mode.

## 🟢 Resolved Issues (Sep.09.15)
*   **Watchdog Precision Audit RESOLVED (R-ID 302)**: Remediated cumulative scheduling drift in `SystemMonitor.kt` by implementing **Fixed Grid Scheduling**. Watchdog pulses are now anchored to the service start monotonic time (`elapsedRealtime`) and aligned to a strict 90s grid. Updated `TrackerService.kt` and `ViewerService.kt` to set the session anchor at initialization.
*   **A15 Hysteresis Audit RESOLVED (R-ID 274)**: Verified that the 10s cooling window (`GNSS_THROTTLING_HYSTERESIS_MS`) in `HardwareProvider.kt` correctly suppresses HUD speed jitter during rapid thermal transitions on Samsung A15 hardware. Confirmed that UI sampling in `MainViewModel.kt` is throttled to 5s for A15 devices to maintain HUD stability.

*(Total: 973 Issues Resolved since inception)*
