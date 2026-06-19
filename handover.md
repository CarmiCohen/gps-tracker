# Handover - v8.9.6

## Technical Summary: Issue #194 Fix
**Issue**: SIT Persistence Packet Loss Risk.
**Affected Components**: `TrackerService.kt`, `ViewerService.kt`, `RemoteHandler.kt`, `EngineConstants.kt`

### Changes:
- **EngineConstants**: Added `SIT_TRANSMISSION_LATCH_MS` (10s) to define the robust transmission window for discrete SIT events.
- **TrackerService**: 
    - Implemented `lastSitSyncLatchTs` to track the start of a detection event.
    - Added `latchedSitDetected` logic to hold the `isSitDetected` flag `true` for the latch duration in both `pushCurrentStatus` and `updateRibbons`.
- **ViewerService**: Included `ALERT_ID_TRACKER_CHAIR` in `activeViolations` to allow reconstruction of forensic markers from status flags.
- **RemoteHandler**: Implemented rising-edge detection for `isTrackerSitDetected` to ensure exactly one log is generated on the Viewer despite the transmission latch.

## Technical Summary: Issue #191 Fix
**Issue**: Muzzle Window Race Condition.
**Affected Components**: `SyncManager.kt`, `TrackerService.kt`, `EngineConstants.kt`

### Changes:
- **SyncManager**: Added `onSyncFinished` listener and invoked it in a `finally` block within `flushPendingUpdates` to ensure the handshake occurs even on failure.
- **TrackerService**: 
    - Migrated from a fixed 500ms timer to a deterministic start/finish handshake with `SyncManager`.
    - Implemented `muzzleReleaseJob` with a 200ms hysteresis delay to allow physical resonance to settle post-I/O.
- **EngineConstants**: Increased `MUZZLE_WINDOW_DURATION_MS` to 2000ms as a conservative safety ceiling.
