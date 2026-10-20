# Resolution Archive - GPS Tracker (Oct20.3 Baseline)

## ISSUE-OCT20.3-F1: Hardware Profile Test Regression (Oct20.3)
- **Resolved**: Oct20.3
- **Description**: `HardwareSuiteProfileTest.kt` failed to compile due to a missing `networkProvider` argument in the `HardwareSuite` constructor, caused by an upstream architectural change (R-ID 742).
- **Remediation**: 
    - **Test Hardening**: Updated `mockSystemStatusProvider` in `HardwareSuiteProfileTest.kt` to include a functional `NetworkProvider` mock.
    - **Interface Compliance**: Implemented missing `registerListener` and `unregisterListener` stubs in the anonymous `NetworkProvider` instance to satisfy the interface contract.
- **Outcome**: 🟢 **PASSED**. Test suite compilation restored and verified. (SOT ID 779).

## ISSUE-OCT19.0-F1: Viewer with Simulated Tracker (Oct20.2)
- **Resolved**: Oct20.2
- **Description**: Validation of the Tracker-Viewer bridge simulation was blocked by non-deterministic mode restoration. If the app was force-stopped in a session, it would auto-resume into that mode, preventing manual landing on the role selection screen.
- **Remediation**:
    - **Initialization Hardening**: Updated `MainViewModel.applyInitialSettings` to zero out `appMode` and `isSystemActive` in the UI state if `isRestorationDisabled` is true. This ensures the app starts on the Landing Screen regardless of the persisted session state.
    - **Validation**: Confirmed that enabling "Skip Mode Restoration" in Diagnostics allows landing on the role selection screen after relaunch. Successfully verified "Virtual Peer Presence" simulation in Viewer mode with telemetry pulses visible on HUD and Map.
- **Outcome**: 🟢 **PASSED**. The simulation bridge for remote validation is fully operational and deterministic. (SOT ID 778).

... [Full resolution history (>350 entries) preserved in active RESOLUTION_ARCHIVE.md] ...
