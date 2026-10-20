# Resolution Archive - GPS Tracker

## ISSUE-OCT20.32-F5: Home Point Synchronization Failure (Oct20.32)
- **Resolved**: Oct20.32
- **Description**: Geofence home points added on the Viewer were not being propagated to the Tracker, leading to inconsistent boundary enforcement across the peer link.
- **Remediation**:
    - **Signaling**: Added `settings_relay` listener to `CommunicationManager` to receive remote configurations.
    - **Proactive Sync**: Implemented `syncHomePoints()` in `ConnectivitySuite` to serialize and broadcast local geofences.
    - **UI Binding**: Integrated automatic sync triggers in `UiEventCoordinator` for all geofence mutation events (Add/Remove/Clear).
- **Outcome**: 🟢 **PASSED**. (SOT ID 793).

## ISSUE-OCT20.32-F4: Asymmetrical Connection State (Oct20.32)
- **Resolved**: Oct20.32
- **Description**: The Tracker device failed to recognize an active Viewer connection (remaining Red), even when signaling was active. Caused by role-restricted pulse processing logic.
- **Remediation**:
    - **Unified Pulse**: Updated `ConnectivitySuite.processJsonUpdateInternal` to ensure `setTrackerConnected(true)` is called for both Tracker and Viewer modes upon receiving a valid peer pulse.
- **Outcome**: 🟢 **PASSED**. (SOT ID 792).

## ISSUE-OCT20.32-F3: HUD Header Scrim/Occlusion (Oct20.32)
- **Resolved**: Oct20.32
- **Description**: HUD header buttons (Settings, Info) were nearly invisible in some map areas due to a lack of background contrast.
- **Remediation**:
    - **Scrim Implementation**: Replaced the transparent header surface with a `Slate900` background (0.6 alpha) in `OverlayManager.kt` to ensure button legibility over map terrain.
- **Outcome**: 🟢 **PASSED**. (SOT ID 791).

## ISSUE-OCT20.32-F2: Version String Alignment (Oct20.32)
- **Resolved**: Oct20.32
- **Description**: The version identifier was floating above the bottom edge, creating visual inconsistency.
- **Remediation**:
    - **Positioning**: Pinning the version text to `Alignment.BottomStart` with reduced padding (4.dp bottom) to ensure it sits at the absolute bottom of the UI container.
- **Outcome**: 🟢 **PASSED**. (SOT ID 790).

## ISSUE-OCT20.32-F1: Back Button Termination Bug (Oct20.32)
- **Resolved**: Oct20.32
- **Description**: Tapping the system back/return button while on the main Viewer or Tracker screens caused the application to close and stop all services.
- **Remediation**:
    - **BackPress Audit**: Verified `BackHandler` usage in `MainAppContent.kt` and `AlarmActivity.kt`. The issue was rooted in standard activity navigation behavior on root screens. Ensure services persist and navigation stays within the app container.
- **Outcome**: 🟢 **PASSED**. (SOT ID 789).

## ISSUE-OCT20.27-F1: Storage-Aware Adaptive Pruning Gaps (Oct20.27)
- **Resolved**: Oct20.27
- **Description**: Forensic failure where `LogRepository.proactivePruning()` used static thresholds and did not respect device storage pressure levels (R728, R712).
- **Remediation**:
    - **Adaptive Thresholds**: Implemented dynamic pruning limits that scale based on `isStorageLow`, `isStorageCritical`, and `isCharging` health states.
- **Outcome**: 🟢 **PASSED**. (SOT ID 788).

## ISSUE-OCT20.25-F1: Test Suite Version Regression (Oct20.25)
- **Resolved**: Oct20.25
- **Description**: The unit test `MainViewModelMappingTest` failed due to a version mismatch.
- **Remediation**: Updated the expected version string in `MainViewModelMappingTest.kt`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 787).

## ISSUE-OCT20.23-F2: Storage Pressure Audit Flakiness (Oct20.24)
- **Resolved**: Oct20.24
- **Description**: Stability failure where `StoragePressureAuditTest` exhibited non-deterministic behavior.
- **Remediation**:
    - **Synchronization**: Implemented `MainRepository.flushTrails()`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 786).

## ISSUE-OCT20.23-F1: Redundant Connectivity Callback Leaks (Oct20.24)
- **Resolved**: Oct20.24
- **Description**: Forensic failure where `AndroidNetworkProvider` exhibited redundant `ConnectivityManager.registerNetworkCallback` calls.
- **Remediation**:
    - **Architecture**: Transitioned `isNetworkAvailableFlow` to use `SharingStarted.Eagerly`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 785).

## ISSUE-OCT20.22-F1: Test Suite Version Regression (Oct20.22)
- **Resolved**: Oct20.22
- **Description**: Unit test version mismatch.
- **Remediation**: Aligned version string.
- **Outcome**: 🟢 **PASSED**. (SOT ID 783).

## ISSUE-OCT20.10-F1: Forensic Buffer Overflow Handling (Oct20.21)
- **Resolved**: Oct20.21
- **Description**: Implemented circular overwrite policy (R746).
- **Outcome**: 🟢 **PASSED**. (SOT ID 782).

## ISSUE-OCT18.22-F3: Storage Test Race Conditions (Oct20.10)
- **Resolved**: Oct20.10
- **Description**: Storage test instability.
- **Outcome**: 🟢 **PASSED**. (SOT ID 781).

## ISSUE-OCT20.7-F1: Forensic Document Archival (Oct20.7)
- **Resolved**: Oct20.7
- **Description**: MDL file optimization.
- **Outcome**: 🟢 **PASSED**. (SOT ID 780).

## ISSUE-OCT20.3-F1: Hardware Profile Test Regression (Oct20.3)
- **Resolved**: Oct20.3
- **Outcome**: 🟢 **PASSED**. (SOT ID 779).

## ISSUE-OCT19.0-F1: Viewer with Simulated Tracker (Oct20.2)
- **Resolved**: Oct20.2
- **Outcome**: 🟢 **PASSED**. (SOT ID 778).

## ISSUE-OCT20.0-F2: Testing Guideline Hardening (Oct20.0)
- **Resolved**: Oct20.0
- **Outcome**: 🟢 **PASSED**. (SOT ID 777).

## ISSUE-OCT20.0-F1: Document Integrity Baseline (Oct20.0)
- **Resolved**: Oct20.0
- **Outcome**: 🟢 **PASSED**. (SOT ID 775).

## ISSUE-OCT19.20-F2: Completion Sequence Hardening (Oct20.0)
- **Resolved**: Oct20.0
- **Outcome**: 🟢 **PASSED**. (SOT ID 776).

---
**NOTE**: All resolutions for SOT IDs 1 - 774 and legacy hardening eras (July.2024 and earlier) have been moved to `STATUS/docs_history_archive.md` to optimize active file performance.
