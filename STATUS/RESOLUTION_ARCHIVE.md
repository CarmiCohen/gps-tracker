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
