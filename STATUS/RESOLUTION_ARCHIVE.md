# Resolution Archive (Sep.11.10)

## 🟢 Resolved Issues (Sep.11.10)
*   **Map Partitioning Integrity RESOLVED (#243-Audit)**: Performed a rigorous integrity audit of the Map State Partitioning implementation.
    *   **Root-Cause Remediation**: Eliminated "leftovers of leftovers" by removing redundant map tool overlays and individual parameters from `TrackerScreen.kt` and `ViewerScreen.kt`. Optimized `MainViewModel.kt` with trigger pruning via `MapUiParts` to ensure map state updates only on relevant changes (R-ID 287).

## 🟢 Resolved Issues (Sep.10.12)
*   **Map State Partitioning RESOLVED (#243)**: Refactored the Map UI layer to use a consolidated `MapViewState` object.
    *   **Root-Cause Remediation**: Bundled ~40 individual map parameters into a structured `MapViewState` in `MainUiState.kt`. Updated `MainViewModel.kt` to aggregate these into a single flow and refactored `AppMapContainer` and `OsmMap` to consume the state, significantly reducing recomposition overhead and improving interface stability (R-ID 287).

## 🟢 Resolved Issues (Sep.10.08)
*   **HUD Aggregator Refactoring RESOLVED (#241)**: Transitioned UI components to subscribe directly to segmented sub-states, eliminating the monolithic `HudState` facade.
    *   **Root-Cause Remediation**: Removed `HudState` from `EngineModels.kt` and `MainViewModel.kt`. Updated `SharedUiComponents.kt`, `TrackerScreen.kt`, and `ViewerScreen.kt` to use `HudConnectivityState`, `HudTelemetryState`, and `HudHealthState` flows directly, significantly narrowing recomposition scope and reducing JIT compilation load (R-ID 286).

## 🟢 Resolved Issues (Sep.10.06)
*   **Unified Termination Logic RESOLVED (#285)**: Centralized session termination button and confirmation flow into `SharedUiComponents.kt`.
    *   **Root-Cause Remediation**: Created `SessionTerminationButton` to replace duplicate, inconsistent implementations in `TrackerScreen` and `ViewerScreen`, ensuring identical visual feedback and logic across roles (R-ID 285).

## 🟢 Resolved Issues (Sep.10.05)
*   **SRV Status Inconsistency RESOLVED (#941)**: Remediated peer status inconsistency (one device RED, one GREEN) during role transitions. 
    *   **Root-Cause Remediation**: Hardened `TelemetryRepository.clear()` and `ConnectivitySuite.stop()` to explicitly reset `isRelayConnected` and `lastRtt` flows, ensuring immediate UI feedback when signaling is terminated (R941).
*   **Legacy Field Cleanup Hardening RESOLVED (#284)**: Remediated `NoSuchMethodError` crashes in `TrackerScreen.kt` and `ViewerScreen.kt` by migrating all direct `LocationUpdate` field accesses to the partitioned state structure (`.kinetic`, `.atmospheric`, `.integrity`).
*   **Peer Status LED Verification RESOLVED (#943)**: Finalized and verified the behavior of Peer Role LEDs (VWR/TRK) in `StatusBar` and `Dashboard`. 
    *   **Logic Enforcement (R972)**: Verified that peer indicators correctly default to **RED** (Rose500) in single-device isolation tests when no remote telemetry is detected.

## 🟢 Resolved Issues (Sep.09.16)
*   **Identity Color Role Confusion RESOLVED (#942)**: Remediated role-color confusion in icons, StatusBar badges, and Dashboard metrics.

*(Total: 978 Issues Resolved since inception)*
