# Resolution Archive (Sep.10.20)

## 🟢 Resolved Issues (Sep.10.20)
*   **Map State Partitioning RIGOROUS AUDIT (#243-Audit)**: Performed a deep-forensic audit of the Map UI layer to eliminate remaining derived state and redundant parameters.
    *   **Root-Cause Remediation**: Consolidated `MapToolsOverlay` and marker freshness logic into `MapViewState`. Moved staleness calculations (15s gate) into the `MainViewModel` flow to ensure UI-side calculations are entirely eliminated. Simplified `AppMapContainer` signature to consume a single state object, fulfilling the strict interpretation of R-ID 287.

## 🟢 Resolved Issues (Sep.11.10)
*   **Map Partitioning Integrity RESOLVED (#243-Audit-Initial)**: Performed an initial integrity audit of the Map State Partitioning implementation.
    *   **Root-Cause Remediation**: Eliminated "leftovers of leftovers" by removing redundant map tool overlays and individual parameters from `TrackerScreen.kt` and `ViewerScreen.kt`. Optimized `MainViewModel.kt` with trigger pruning via `MapUiParts` to ensure map state updates only on relevant changes (R-ID 287).

## 🟢 Resolved Issues (Sep.10.12)
*   **Map State Partitioning RESOLVED (#243)**: Refactored the Map UI layer to use a consolidated `MapViewState` object.
    *   **Root-Cause Remediation**: Bundled ~40 individual map parameters into a structured `MapViewState` in `MainUiState.kt`. Updated `MainViewModel.kt` to aggregate these into a single flow and refactored `AppMapContainer` and `OsmMap` to consume the state, significantly reducing recomposition overhead and improving interface stability (R-ID 287).

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

*(Total: 979 Issues Resolved since inception)*
