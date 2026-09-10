# Resolution Archive (Sep.10.05)

## 🟢 Resolved Issues (Sep.10.05)
*   **SRV Status Inconsistency RESOLVED (#941)**: Remediated peer status inconsistency (one device RED, one GREEN) during role transitions. 
    *   **Root-Cause Remediation**: Hardened `TelemetryRepository.clear()` and `ConnectivitySuite.stop()` to explicitly reset `isRelayConnected` and `lastRtt` flows, ensuring immediate UI feedback when signaling is terminated (R941).
*   **Legacy Field Cleanup Hardening RESOLVED (#284)**: Remediated `NoSuchMethodError` crashes in `TrackerScreen.kt` and `ViewerScreen.kt` by migrating all direct `LocationUpdate` field accesses to the partitioned state structure (`.kinetic`, `.atmospheric`, `.integrity`).
*   **Peer Status LED Verification RESOLVED (#943)**: Finalized and verified the behavior of Peer Role LEDs (VWR/TRK) in `StatusBar` and `Dashboard`. 
    *   **Logic Enforcement (R972)**: Verified that peer indicators correctly default to **RED** (Rose500) in single-device isolation tests when no remote telemetry is detected.

## 🟢 Resolved Issues (Sep.09.16)
*   **Identity Color Role Confusion RESOLVED (#942)**: Remediated role-color confusion in icons, StatusBar badges, and Dashboard metrics.

## 🟢 Resolved Issues (Sep.09.15)
*   **Watchdog Precision Audit RESOLVED (R-ID 302)**: Remediated cumulative scheduling drift in `SystemMonitor.kt` by implementing **Fixed Grid Scheduling**. Watchdog pulses are now anchored to the service start monotonic time (`elapsedRealtime`) and aligned to a strict 90s grid.

*(Total: 974 Issues Resolved since inception)*
