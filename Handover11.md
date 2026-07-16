# Project Handover: Issue #516 (De-duplicate "Status" Logic) - STARTED

## Status: IN-PROGRESS
**Version Context**: `July.16.18`
**Current Task**: Consolidation of redundant system status flags into a unified `SystemHealthState`.

## Context & Progress
This issue follows the successful flattening of the service architecture (Issue #513) and the simplification of the GPS manager (Issue #514). The project is now lean enough to address the overlapping "status" models that exist in both the service and UI layers.

### 1. Analysis Completed
- **Current Models**:
    - `IntegrityState` (in `Models.kt`): Used by `IntegrityMonitor` and `TelemetryRepository`. Contains ~25 flags (signalLoss, gpsStalled, batteryLevel, etc.).
    - `TrackerStatus` (in `Models.kt`): Used for telemetry persistence and transmission. Overlaps 90% with `IntegrityState`.
    - `LocationState` (in `Models.kt`): Used for UI state. Redundant with `TrackerStatus`.
    - `IntegrityStateUi`: Another UI-specific slice.
- **Redundancy Observed**: The same data (e.g., `isPowerSaveMode`, `isStalled`, `batteryLevel`) is being tracked and passed through multiple distinct data classes, leading to synchronization overhead and potential inconsistencies.

### 2. Implementation Plan for Issue #516
- **Step 1**: Create `SystemHealthState` in `:core:engine` to serve as the authoritative model for all device metadata.
- **Step 2**: Refactor `IntegrityMonitor` to produce `SystemHealthState`.
- **Step 3**: Update `TelemetryRepository` to store a single `SystemHealthState` rather than fragmented flows.
- **Step 4**: Transition `ConnectivitySuite` to use `SystemHealthState` for telemetry assembly.
- **Step 5**: Purge `IntegrityState`, `IntegrityStateUi`, and reduce `LocationState` to pure position data.

## Project State Snapshot
- **Connectivity**: `ConnectivitySuite` is fully operational, managing network and peer state.
- **GPS**: `GpsManager` is simplified, relying on Fused Location. SNR buffering is removed.
- **DI**: Manual `AppContainer` is stable.
- **Build**: Successful `:app:assembleDebug`.

## Instructions for Resumption
1.  Open `app/src/main/java/com/gps19/app/Models.kt` and identify all properties in `IntegrityState` that belong in the new engine-level health model.
2.  Create `SystemHealthState.kt` in `core/engine/src/main/java/com/gps19/core/engine/`.
3.  Migrate `IntegrityMonitor.kt` polling logic to populate the new model.
4.  Rigorously remove the old `IntegrityState` from `TelemetryRepository.kt`.
