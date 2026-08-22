# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 703**

## 100. Offline Storage Hardening & R197 Compliance (Aug.22.03)
*   **Issue #197: Offline Storage Hardening**.
    - **Resolution**: Aligned `PendingStatusDao` and `OfflineRepository` with R197 chunked pruning standards. Replaced monolithic `prune()` with `getPruneThreshold` and `pruneByThreshold` calls.
    - **Verification**: Verified logic via source audit. Prevents I/O stalls during large accumulation recovery on budget hardware (Samsung A15).

## 99. Core Engine Integrity & Geofence Hysteresis (Aug.22.02)
*   **Issue #308: Restored Core Engine Definitions**.
    - **Resolution**: Re-implemented the missing core data structures (`AlarmEvaluationState`, `ProcessedLocation`, `SpatialAnchor`, and `RejectedPoint`) in `EngineModels.kt`. This resolved the critical build blocker and enabled verification of Chapter 11.2 (Geofence Recovery Hysteresis).
    - **Verification**: Verified via `MainAlarmLogicTest` and `GeofenceBatteryAuditTest` (33 tests passing).

## 98. Navigation & JNI Hardening (Aug.22.00)
*   **Issue #250: Navigation Backstack Inconsistency**.
    - **Resolution**: Hardened the navigation logic in `MainAppContent.kt` to eliminate "Ignoring popBackStack to route landing" warnings. Replaced `popUpTo(0)` with explicit graph-relative `popUpTo(Screen.Landing.route)` and added `launchSingleTop = true` to all destination transitions.
*   **Issue #301: JNI Watchdog Implementation**.
    - **Resolution**: Implemented a robust JNI watchdog in `JdHardwareManager.kt` using `withTimeout(Dispatchers.IO)`. Native synchronization and initialization calls are now protected by a 2000ms timeout to prevent hardware-level hangs from causing ANRs or engine stalls. Updated `TrackerService` to await the suspending `syncState` call.

## 97. Performance & JNI Remediation (Aug.21.09)
*   **Issue #248: UI Thread Stall Remediation**.
    - **Resolution**: Implemented granular flow segmentation in `MainViewModel.kt` using a specialized `HudUiParts` data class and `distinctUntilChanged()`. This eliminates the 1070ms UI thread stall by pruning redundant HudState aggregation triggers during telemetry hydration.
*   **Issue #265: JNI Startup Optimization**.
    - **Resolution**: Migrated `JdHardwareManager` to a coroutine-safe `suspend initialize()` pattern. Native library loading and registration are now offloaded to `Dispatchers.IO` with a `Mutex` to prevent UI thread blocking during bootstrap.
*   **Issue #249/262: Native Resource Hardening**.
    - **Resolution**: Implemented missing `n6` (nativeRelease) in `jdhardware-jni.cpp`. Added explicit lifecycle disposal calls in `TrackerService.onDestroy()` and `ViewerService.onDestroy()` to clear native pointers and prevent `BaseEventQueue.dispose` failures.
*   **Issue #257/271: Samsung I/O Mitigation**.
    - **Resolution**: Aligned background maintenance in `BaseMonitorService.kt` with the 15s `STAGGERED_IO_PRUNING_DELAY_MS`. This eliminates I/O competition with Samsung's Kumiho package auditing during the launch window.

*(Older resolutions preserved in Git history)*
