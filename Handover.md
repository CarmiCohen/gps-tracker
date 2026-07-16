# Project Handover: Service Simplification Phase (Issues #513, #514) - COMPLETED

## Status: COMPLETED
**Version Context**: `July.16.18`

This session focused on the core simplification of the service layer and hardware management, adhering to the R406 hardening plan.

## Key Changes

### 1. ConnectivitySuite Integration (Issue #513)
- **Consolidation**: Merged `AppNetworkManager`, `SyncManager`, and `RemoteHandler` into a unified `ConnectivitySuite.kt`.
- **Decoupling**: Removed `RemoteUpdateWrapper` and implemented a direct `PeerListener` interface.
- **Dependency Graph**: Updated `AppContainer`, `TrackerService`, and `ViewerService` to use the new suite, significantly reducing constructor bloat.

### 2. GpsManager Simplification (Issue #514)
- **Streamlined Hardware Access**: Refactored `GpsManager.kt` to rely on `FusedLocationProviderClient` for location updates and immediate `GnssStatus` for metadata.
- **Forensic Cleanup**: Removed legacy `kickGps`/`reviveGps` commands and the high-maintenance SNR sampling/buffering logic.
- **Downstream Updates**: 
    - Updated `TelemetryAggregator.kt` in `:core:engine` to remove SNR-based gap filling.
    - Updated `HistoryManager.kt` to purge SNR sampling dependencies.
    - Removed `EngineSnrSample` from `EngineModels.kt`.

## Verification Results
- **Build**: Successfully executed `:app:assembleDebug`.
- **Authoritative Requirements**: Updated `STATUS/SOT_MASTER_REQUIREMENTS.md` with **R406h** (Connectivity) and **R406i** (GPS).
- **Tracking**: `issues.md` and `SIMPLIFICATION_PLAN.md` updated.

## Next Steps
- **Issue #516**: De-duplicate "Status" logic by creating a unified `SystemHealthState`.
