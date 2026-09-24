# 🏛️ Resolution Archive - Sep.24.70

## 🏁 Issue #1272: Alarm Notification Leak in Tracker Mode
*   **Resolved**: Sep.24.70
*   **Root Cause**: `AppAlarmManager.restoreState` returned early if the input JSON was empty, failing to clear the in-memory `activeAlarms` map. When switching from Tracker to Viewer role, stale alarm state from the previous role persisted until the next evaluation cycle. Since `shouldPlaySiren` is gated by `isTrackerMode`, the siren was suppressed in Tracker mode but triggered immediately upon switching to Viewer mode before a fresh evaluation could occur.
*   **Remediation**:
    *   **AppAlarmManager.kt**: Hardened `restoreState` to ensure the `activeAlarms` map is cleared before any conditional early-return.
    *   **AppAlarmManager.kt**: Updated `restoreLogicState` to explicitly sync and reset the `isTrackerMode` gating flag during role transitions.
    *   **AppAlarmManager.kt**: Ensured that `evaluateAlarms` correctly updates the `isTrackerMode` flag from the `AlarmServiceContext` to prevent role-based state leakage.
*   **R-ID**: 467

# 🏛️ Resolution Archive - Sep.24.60

## 🏁 Issue #1308: Missing Forensics Trace Collection in ViewerService
*   **Resolved**: Sep.24.60
*   **Root Cause**: `ViewerService` lacked the high-precision `forensicSamplingLoop` present in `TrackerService`. While it performed stability audits via `ForensicAuditor`, it failed to capture the detailed environmental, spatial, and IMU traces required for a complete audit of the monitor device's integrity. This created a forensic asymmetry where local tampering or environment-based reliability issues on the Viewer could not be analyzed with the same granularity as the Tracker.
*   **Remediation**:
    *   **ViewerService.kt**: Implemented a role-isolated `forensicSamplingLoop` utilizing a buffered `Channel<Boolean>` for trigger coordination.
    *   **ViewerService.kt**: Integrated spike-aware capture logic (`performForensicCapture`) that decouples physical IMU and spatial jumps from the adaptive sampling rate.
    *   **ViewerService.kt**: Added thermal recovery latency audits to track sensor stabilization periods after cooling mode exits.
    *   **ViewerService.kt**: Hardened the service teardown by explicitly canceling the sampling job in `onDestroy()`.
*   **R-ID**: 466

# 🏛️ Resolution Archive - Sep.24.50

## 🏁 Issue #1245: Non-Blocking History Flush on Service Termination
*   **Resolved**: Sep.24.50
*   **Root Cause**: `BaseMonitorService.onDestroy()` utilized a synchronous `runBlocking` block to flush the history buffer to the database. During high-load scenarios or database contention, this blocked the service teardown on the Main thread, potentially triggering OS watchdog kills or ANRs during app termination.
*   **Remediation**:
    *   **BaseMonitorService.kt**: Injected `@ApplicationScope` `CoroutineScope`.
    *   **BaseMonitorService.kt**: Refactored `onDestroy()` to launch the `repository.flushHistory()` call within the `applicationScope` on `Dispatchers.IO`.
    *   **BaseMonitorService.kt**: Implemented a strict 2000ms `withTimeout` gate within the teardown coroutine to ensure the process eventually exits even if the database is unresponsive, while logging the outcome for forensic audit.
*   **R-ID**: 465

# 🏛️ Resolution Archive - Sep.24.40

## 🏁 Issue #1241: Functional Restoration of History Sync Streams in ViewerService
*   **Resolved**: Sep.24.40
*   **Root Cause**: `ViewerService` lacked a subscription to the `HistoryManager.historyEvents` stream. Instead, it was relying on a legacy pulse listener that didn't capture history backfill or sync-specific events. This resulted in an observation gap on the monitor device where legitimate history lifecycle events (logs and triggers) were not being surfaced.
*   **Remediation**:
    *   **ViewerService.kt**: Implemented `observeHistoryEvents()` which subscribes to `historyManager.historyEvents` using `collectLatest`.
    *   **ViewerService.kt**: Integrated the observer into the `onServiceInitialize()` sequence to ensure immediate stream connectivity upon service startup.
*   **R-ID**: 464

# 🏛️ Resolution Archive - Sep.24.30

## 🏁 Issue #1307: Forensic Sampling Bottleneck During Rapid Event Sequences
*   **Resolved**: Sep.24.30
*   **Root Cause**: The `forensicSamplingLoop` in `TrackerService` used a conflated channel and a fixed `delay(delayMs)` following each capture iteration. If multiple physical spikes (acoustic or light) occurred during the delay period, subsequent high-priority triggers were dropped or significantly delayed, as the loop could only process one trigger per sampling interval. This created a data-loss risk for rapid tampering event sequences.
*   **Remediation**:
    *   **TrackerService.kt**: Refactored `forensicTriggerChannel` from a conflated channel to a buffered `Channel<Boolean>`.
    *   **TrackerService.kt**: Updated the `forensicSamplingLoop` to utilize a non-blocking `withTimeoutOrNull(delayMs)` polling pattern. This allows the loop to respond to high-priority spike signals (`true`) immediately, bypassing the standard interval timer, while still maintaining the expected sampling cadence for regular state captures.
*   **R-ID**: 463

# 🏛️ Resolution Archive - Sep.24.20

## 🏁 Issue #1256: Monotonic Latch Staleness Across Reboots
*   **Resolved**: Sep.24.20
*   **Root Cause**: Timing latches such as siren cooldowns and violation timers persisted via monotonic references (`elapsedRealtime`) became stale and invalid when a device reboot occurred. Since `elapsedRealtime` resets back to zero upon device restart, old high-valued latches from the prior boot session stayed valid relative to the new boot cycle's timing authority, resulting in long-term false lockouts or feature suppression ("Permanent Muzzle" bug).
*   **Remediation**:
    *   **TimeProvider.kt**: Added a `getBootId()` definition to the temporal authority interface.
    *   **AndroidTimeProvider.kt**: Implemented `getBootId()` to read and cache the kernel's unique session identifier from `/proc/sys/kernel/random/boot_id`, fallback to random UUID if unavailable.
    *   **AppAlarmManager.kt**: Enhanced `restoreLogicState()` to compare the current session's boot identifier with the stored reference. If a reboot is detected, it completely invalidates obsolete monotonic latches across all functional roles, restoring safe baseline operation immediately.
*   **R-ID**: 462
