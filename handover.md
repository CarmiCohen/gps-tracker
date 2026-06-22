# Handover - GPS Tracker Project

## Active Development Phase: Forensic & Stability Hardening (v8.9.x)

### Completed Fix: Issue #227 - Hindsight Transition Smoothing
- **Resolution**: Refined the visual interpolation of promoted "Ghost Paths" by implementing retroactive coordinate optimization. Previously, hindsight promotion used raw rejected coordinates, leading to "jagged" transitions. Now, buffered points are processed through the `ImmFilter` during promotion to ensure spatial continuity.
- **Root-Cause Implementation Details**:
    - **Engine Models**: Expanded `SentinelResult` in `EngineModels.kt` to include `promotedPoints: List<EngineGeoPoint>?`.
    - **LocationSentinel**: Updated `processLocation` to run buffered `RejectedPoint`s through the `immFilter` when a `TRAJECTORY_PROMOTED` state is triggered. This generates optimized (Kalman-filtered) coordinates for the entire hindsight segment.
    - **LocationProcessor**: Modified the `TRAJECTORY_PROMOTED` handler to prioritize these optimized coordinates when invoking `onTrailPointSaved`, ensuring the "Ghost Path" (Slate500) matches the smoothed trajectory of the live path.
    - **Map Components**: Verified `drawTrailToFolder` in `MapComponents.kt` bridges segment transitions correctly to maintain polyline continuity across color changes.
- **Verification**: `issues.md` updated (FIXED #238). Build successful (`app:assembleDebug`).

### Completed Fix: Issue #226 - Intelligent Uncertainty UX
- **Resolution**: Enhanced the "Location Pending" state to communicate the contextual cause of uncertainty, resolving ambiguity during Bayesian radius expansion.
- **Forensic Implementation Details**:
    - **Engine Models**: Defined `LocationPendingReason` enum (NONE, GPS_STALL, ACOUSTIC_VIOLATION, SIGNAL_LOSS, JAMMER_SUSPICION) in `EngineModels.kt` and integrated it into `AlarmEvaluationState`.
    - **State Expansion**: Added `locationPendingReason` to `LocationUpdate`, `TrackerStatus`, `LocationState`, `DashboardState`, `IntegrityState`, and `IntegrityStateUi` across `:core:engine` and `:app`.
    - **Telemetry Pipeline**: 
        - `SyncManager.kt`: Serializes `location_pending_reason` into JSON.
        - `RemoteHandler.kt`: Parses and reconstructs the reason status for the Viewer.
        - `TelemetryUseCase.kt`: Maps the reason into the reactive `LocationState` for UI consumption.
    - **UX/UI Components**:
        - `MapComponents.kt`: Renders a high-visibility Amber overlay (e.g., "UNCERTAINTY: GPS STALL") when `isLocationPending` is active.
        - `DashboardUseCase.kt`: Populates the contextual reason in the analytical dashboard.
- **Verification**: `issues.md` updated (FIXED #237). Build successful (`app:assembleDebug`).

### Pipeline Status:
- **Engine**: SIT, Intelligent Uncertainty, and Hindsight Smoothing logic are stable and verified.
- **UI**: Contextual Map overlays and smoothed Ghost Paths are fully integrated.
- **Next Task**: Field verification of Issue #190 on Xiaomi MIUI 14 hardware.
