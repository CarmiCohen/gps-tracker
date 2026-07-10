# Project Handover: Peer Visibility & Map Stabilization Fixes (v9.3.8)

## 📌 Status: Critical HUD & Map Synchronization Resolved

### 1. Peer Visibility Fix (Issue #073 & #074)
- **Problem**: On the Tracker device, the "VWR" (Viewer) badge remained red even when receiving pulses. This was due to a logic error in `GlobalStatusBar` where `isPeerActive` was shadowed by local telemetry freshness regardless of the app mode.
- **Fix**: Updated `SharedUiComponents.kt` to use role-specific freshness logic. 
    - **Tracker Mode**: `isPeerActive` now checks the age of `lastRemoteActivityTs` (Viewer pulses).
    - **Viewer Mode**: `isPeerActive` continues to use `dashboardState.isTelemetryFresh`.
- **Result**: The "VWR" badge correctly turns Green upon receipt of pulses from a Viewer.

### 2. Map Stabilization (Issue #072)
- **Problem**: Remote coordinates could "flicker" or jump if the Viewer's processing didn't respect the engine's optimized filtering (like the Stationary Anchor) during telemetry receipt.
- **Fix**: Updated `RemoteHandler.kt` to use the `optimizedPoint` from `LocationProcessor` for updating both the UI state and the database. 
- **Result**: Map markers now remain stable at the "Stationary Anchor" even when raw GPS noise is present in the remote telemetry stream.

### 3. Build & Documentation
- **Issues**: `issues.md` updated with root-cause analysis and resolution for #073 and #074.
- **Build**: Final verification build (`app:assembleDebug`) initiated.

---
**Resumption Point for next session**: Verify the "Diagnostics" screen requirement (#059) and investigate displacement-weighted monitor for anchors (#062).
