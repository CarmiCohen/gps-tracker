# Resolution Archive

## [Oct13.8] - Pruning Scheduler & UI Staleness Hardening
**Issue ID**: #SIMP-OCT12.18-S1 (Pruning), #ISSUE-OCT12.13-1 (Staleness), #SIMP-OCT12.17-S1 (Re-Zeroing)
**Status**: RESOLVED
**Description**: Migrated legacy background maintenance to WorkManager and harmonized UI thresholds with staggered polling.
**Root Cause**: 
- **Pruning Contention**: Proactive pruning tasks were running in the foreground lifecycle (UI/Services), causing SQLite write-lock contention during critical tracking windows.
- **False Alarms**: Signal loss thresholds (30s) were shorter than the Screen-Off polling interval (45s), triggering false "Red Screen" alerts.
**Remediation**:
- **Scheduler Integration**: Implemented `PruningWorker` with `DeviceIdle` constraints; removed legacy `proactivePruning()` calls from `MainViewModel` and `BaseMonitorService`.
- **Reactive Re-Zeroing**: Refined `HardwareSuite.kt` with a `gpsRestartTrigger` (SharedFlow) to silently restart the location provider every 6h if accuracy > 30m.
- **Threshold Scaling**: Increased `VIEWER_SIGNAL_LOSS_THRESHOLD_MS` to 90s to provide a 2x buffer for power-save intervals.

## [Oct13.7] - Forensic Isolation & Pointer Stability
**Issue ID**: #ISSUE-OCT13.5-3 (Drain Deficit), #ISSUE-OCT13.6-1 (Isolation)
**Status**: RESOLVED
**Description**: Resolved the 755/1000 forensic under-count and eliminated test leakage between instrumentation runs.
**Root Cause**: 
- **Drain Deficit**: The read-pointer in `ForensicSpillBuffer` would "stall" if a trace failed CRC validation or was filtered as a duplicate, as the commit only accounted for valid entities.
- **Isolation Leak**: `LogRepository` started background coroutines in `init` that would persist buffer remnants from previous tests before the `@Before` block could clear the database.
**Remediation**:
- **Pointer Progress**: Introduced `PeekResult` to track `processedCount` independently of validity; the drainer now commits the total count peeked.
- **Strict Isolation**: Made `stopBackgroundTasks()` and `resetForTest()` suspendable with `cancelAndJoin()` to ensure previous sessions are fully dead before setup.
- **Aggressive Draining**: Added `isTestMode` logic to `LogRepository` to ignore polling intervals and use max batch size during tests.
- **Visibility**: Shifted forensic logs to `Log.i` for guaranteed visibility in logcat.

## [Oct13.5] - Forensic Pipeline Hardening & Batch Verification
**Issue ID**: #ISSUE-OCT13.5-1 (Batching), #ISSUE-OCT13.5-2 (Deduplication)
**Status**: RESOLVED
... (Rest of the file preserved)
