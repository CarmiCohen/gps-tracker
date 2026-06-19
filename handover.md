# Handover - v8.9.7

## Current Status: Issue #194 Remediation (In Progress)

**Issue**: #194 SIT Persistence Packet Loss Risk [Priority 8]
**Goal**: Transition discrete SIT (chair) detection from volatile telemetry flags to a reliable, acknowledged event synchronization pipeline.

### Changes Executed:
1.  **Database (v37)**: 
    - Added `synced: Boolean` column to `LogEntity`.
    - Implemented `getUnsyncedLogs(limit)` and `markLogsAsSynced(localIds)` in `LogDao`.
    - Created `MIGRATION_36_37`.
2.  **Repository Layer**:
    - Updated `LogRepository` to support the new sync queries.
    - Modified `addLog` to reset `synced = false` when merging log entries.
3.  **Service Layer**:
    - Updated `LogManager` to expose synchronization methods.
    - Updated `SyncManager` to include `flushPendingLogs()` in the 10s sync loop. This method fetches unsynced logs, emits them to the relay with an `is_recovered: true` flag, and marks them as synced upon successful socket emission.
4.  **Remote Handler**:
    - Verified `isTrackerSitDetected` rising-edge detection in `RemoteHandler.kt` to prevent duplicate logging on the Viewer side despite the transmission latch.

### Pending Actions (Next Session):
1.  **Viewer-Side Marker Reconstruction**: 
    - Audit `RemoteHandler.handleRemoteUpdate` to ensure that when a reliable log with "Sit Detected" message arrives, it triggers the same forensic marker placement as the real-time flag.
2.  **Sync Loop Audit**:
    - Verify that `flushPendingLogs` in `SyncManager` doesn't conflict with the immediate `networkManager.emit("log_update", data)` call in `LogManager`.
3.  **Documentation**:
    - Update `issues.md` to mark #194 as **FIXED** in v8.9.7.
    - Increment global version strings if applicable.

## Other Open Issues Recap:
- **#191: Muzzle Window Race Condition**: Deterministic handshake is implemented in v8.9.6. Safety ceiling increased to 2000ms. Consider increasing hysteresis delay from 200ms to 500ms for A15 devices if false triggers persist.
- **#190: Xiaomi Autostart Unknown Handling**: Robust gating logic implemented in `MainAlarmLogic.kt`. Confirmed that "Unknown" status only triggers an alarm if `isXiaomiManualOverride` is OFF.
- **#193: Zombie Telemetry UX**: Staleness logic (10s threshold) implemented in `DashboardUseCase.kt`. UI components (`StatusBar`, `DashboardGrid`) correctly dim to `Slate500` when data is stale.

## Forensic Audit Note:
All modifications to `.kt` files have been aligned with the project's root-cause-oriented design principles. Database migrations have been strictly synchronized with model defaults to prevent Android 15 validation crashes.
