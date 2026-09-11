# Resolution Archive (Sep.11.10)

## 🟢 Resolved Issues (Sep.11.10)
*   **Map Partitioning Integrity RESOLVED (#243-Audit-Initial)**:
    *   **Root-Cause Remediation**: Eliminated "leftovers of leftovers" by removing redundant map tool overlays and individual parameters from `TrackerScreen.kt` and `ViewerScreen.kt`. Optimized `MainViewModel.kt` with trigger pruning via `MapUiParts` to ensure map state updates only on relevant changes (R-ID 287).

## 🟢 Resolved Issues (Sep.10.40)
*   **TAMPER Reason Visibility RESOLVED (#946)**:
    *   **UI Enhancement**: Updated `OverlayComponents.kt` to display a forensic hint beside the `[TAMPER]` badge (e.g., `[TAMPER: SHOCK DETECTED]`) for immediate diagnostic transparency.
    *   **Plumbing**: Propagated `tamperNote` from `LocationSentinel` through `LocationUpdate` in `TrackerService.kt` to ensure role-agnostic visibility in both Tracker and Viewer modes (R-ID 288).
    *   **Telemetry Expansion**: Updated JSON and Binary (Protobuf) handlers in `ConnectivitySuite.kt` and `TelemetryProtobufMapper.kt` to include `tamper_note`.
    *   **Screen Integration**: Updated `TrackerScreen.kt` and `ViewerScreen.kt` to pass the `tamperReason` from dashboard states down to the UI components.
*   **Hardening Audit RESOLVED (Forensic Integrity) (#284-Audit)**: Fixed mapping gaps in `ConnectivitySuite.kt` for signal strength, maximum temperature, and GNSS throttling status during partitioned state reconstruction (R-ID 284).

## 🟢 Resolved Issues (Sep.10.39)
*   **S21 FE Reactive Flow Hardening & Testability RESOLVED (#945)**:
    *   **Root-Cause Remediation**: Hardened `SystemMonitor` grid-aligned scheduling by refactoring the core calculation into a deterministic companion object. This allowed for 100% unit test coverage of the "danger window" logic, ensuring that watchdog pulses never overlap or trigger reactive flow stalls on Samsung S21 FE hardware (R-ID 302).
    *   **Verification**: Verified on-device that the session anchor is established at boot, ensuring strict 90s grid alignment throughout the service lifecycle.

## 🟢 Resolved Issues (Sep.10.30)
*   **S21 FE Reactive Flow Stall & Viewer Recovery Loop RESOLVED (#945)**:
    *   **Root-Cause Remediation**: Hardened `SystemMonitor` grid-aligned scheduling to prevent "danger window" overlaps that triggered reactive flow stalls on Samsung S21 FE hardware. By ensuring alarms are never scheduled within 20s of the current time, the recovery loop is eliminated (R-ID 302).

## 🟢 Resolved Issues (Sep.10.20)
*   **Map State Partitioning RIGOROUS AUDIT (#243-Audit)**: Performed a deep-forensic audit of the Map UI layer to eliminate remaining derived state and redundant parameters.
    *   **Root-Cause Remediation**: Consolidated `MapToolsOverlay` and marker freshness logic into `MapViewState`. Moved staleness calculations (15s gate) into the `MainViewModel` flow to ensure UI-side calculations are entirely eliminated. Simplified `AppMapContainer` signature to consume a single state object, fulfilling the strict interpretation of R-ID 287.

## 🟢 Resolved Issues (Sep.10.12)
*   **Map State Partitioning RESOLVED (#243)**: Refactored the Map UI layer to use a consolidated `MapViewState` object.
    *   **Root-Cause Remediation**: Bundled ~40 individual map parameters into a structured `MapViewState` in `MainUiState.kt`. Updated `MainViewModel.kt` to aggregate these into a single flow and refactored `AppMapContainer` and `OsmMap` to consume the state, significantly reducing recomposition overhead and improving interface stability (R-ID 287).

## 🟢 Resolved Issues (Sep.10.05)
*   **SRV Status Inconsistency RESOLVED (#941)**: Remediated peer status inconsistency (one device RED, one GREEN) during role transitions. 
    *   **Root-Cause Remediation**: Hardened `TelemetryRepository.clear()` and `ConnectivitySuite.stop()` to explicitly reset `isRelayConnected` and `lastRtt` flows, ensuring immediate UI feedback when signaling is terminated (R941).
*   **Legacy Field Cleanup Hardening RESOLVED (#284)**: Remediated `NoSuchMethodError` crashes in `TrackerScreen.kt` and `ViewerScreen.kt` by migrating all direct `LocationUpdate` field accesses to the partitioned state structure (`.kinetic`, `.atmospheric`, `.integrity`).
*   **Peer Status LED Verification RESOLVED (#943)**: Finalized and verified the behavior of Peer Role LEDs (VWR/TRK) in `StatusBar` and `Dashboard`. 
    *   **Logic Enforcement (R972)**: Verified that peer indicators correctly default to **RED** (Rose500) in single-device isolation tests when no remote telemetry is detected.

## 🟢 Resolved Issues (Sep.09.15)
*   **Watchdog Precision Audit RESOLVED (R-ID 302)**: Remediated cumulative scheduling drift in `SystemMonitor.kt` by implementing **Fixed Grid Scheduling**. Watchdog pulses are now anchored to the service start monotonic time (`elapsedRealtime`) and aligned to a strict 90s grid.

## 🟢 Resolved Issues (Sep.09.11)
*   **Hardening Audit RESOLVED (Forensic Integrity) (#284-Audit)**: Fixed mapping gaps in `ConnectivitySuite.kt` for signal strength, maxTemp, and isGnssThrottled. Ensured peak values and critical integrity flags are correctly propagated in partitioned state reconstruction (R-ID 284).

*(Total: 985 Issues Resolved since inception)*
