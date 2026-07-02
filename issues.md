# Project Issues

## Open Issues

### Issue #016: Main Thread Performance Bottlenecks (UI Jank)
- **Description**: System reports skipped frames (e.g., 43 frames) during map interaction and app startup.
- **Root Cause**: Potential heavy lifting on the main thread during `OsmMap` rendering or high-frequency `getPackageName` calls.
- **Status**: Open
- **Priority**: Medium
- **Target**: `MapComponents.kt`, `MainActivity.kt`.

### Issue #017: Compose SnapshotStateList Lock Verification Failures
- **Description**: Runtime warnings regarding `SnapshotStateList` failing lock verification and running slower.
- **Root Cause**: Non-optimized dex code or Proguard/R8 optimizations affecting Compose internal state management.
- **Status**: Open
- **Priority**: Low
- **Target**: Global / Build Configuration.

### Issue #018: Tracker Behavior Stability - Inaccurate "JUMPING" states
- **Description**: Tracker state switches to `JUMPING` with high speeds (e.g., 11.1) while stationary or during low-speed movement.
- **Root Cause**: Location noise or filtering logic sensitivity in the core engine.
- **Status**: Open
- **Priority**: Medium
- **Target**: `core:engine`.

## Resolved Issues

### Issue #019: Android 14+ "While-in-Use" Permission Transition Risk
- **Description**: Services may fail to add `MICROPHONE` type if they transition from background to "active monitoring" without a fresh UI pulse.
- **Root Cause**: Android 14's strict enforcement of `FOREGROUND_SERVICE_TYPE_MICROPHONE` requiring a visible activity or a specific allowed transition.
- **Implementation**: 
    * Added `isRecentUiPulse()` to `BaseMonitorService` to bridge the gap between background start and UI visibility.
    * Refined `TrackerService.getAvailableForegroundServiceType` to claim `MICROPHONE` if a UI pulse was received within `UI_PULSE_TIMEOUT_MS`.
    * Ensured `updateForegroundServiceType()` is called immediately after async setup completion.
- **Status**: Resolved
- **Priority**: Medium
- **Target**: `TrackerService.kt`, `BaseMonitorService.kt`.

### Issue #015: StandaloneCoroutine Cancellation during Service Start
- **Description**: `JobCancellationException` observed immediately after foreground service failure.
- **Root Cause**: Coroutine scopes tied to service lifecycle were being cancelled when the service failed to transition to the foreground, and generic catch blocks were logging these cancellations as errors.
- **Implementation**: 
    * Hardened `SyncManager`, `RemoteHandler`, and `CommandRouter` to explicitly ignore `CancellationException` in generic catch blocks.
    * Ensured `CoroutineExceptionHandler` in `CommandRouter` filters out cancellation noise.
    * Verified FGS update loops in `TrackerService` and `ViewerService` preserve cancellation intent without logging.
- **Status**: Resolved
- **Priority**: High
- **Target**: `TrackerService.kt`, `ViewerService.kt`, `SyncManager.kt`, `RemoteHandler.kt`, `CommandRouter.kt`.

### Issue #013: Forensic UI Expansion - Stationary Scaling Visibility
- **Description**: Expose internal stationary scaling metrics to the UI telemetry panel.
- **Status**: Resolved
- **Priority**: Medium

### Issue #014: Foreground Service Type Mismatch (Android 14+)
- **Description**: Logcat reports `Failed to update foreground service type`.
- **Root Cause**: Mismatch between `AndroidManifest.xml` foreground types and the `startForeground()` call. Android 14 also forbids adding `MICROPHONE` types from the background.
- **Implementation**: 
    * Hardened `BaseMonitorService.safeStartForeground` to enforce `LOCATION` type on API 34+.
    * Added logic in `TrackerService` to only claim `MICROPHONE` if the UI is visible or it was already active, preventing illegal background transitions.
    * Improved exception handling to preserve `CancellationException`.
- **Status**: Resolved
- **Priority**: High
- **Target**: `TrackerService.kt`, `ViewerService.kt`.
