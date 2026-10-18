# Resolution Archive

## ISSUE-OCT18.8-F1: Urban Multi-path Hardening (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: GNSS reflections in "urban canyons" often present with high SNR (Signal-to-Noise Ratio), misleading traditional jump filters. Strong but inaccurate reflections could trigger false geofence alerts or cause the stationary anchor to break out prematurely.
- **Remediation**:
    - **SNR Latch (R-ID 735)**: Implemented a 6-minute (360s) security hold for jumps that present with SNR ≥ 35. This ensures the system remains in a "hold" state long enough for the reflection burst to clear.
    - **High-SNR Reflection Detection**: Updated `PhysicsUtils.isVisualJump` to detect and flag high-SNR reflections (Potential Multipath).
    - **Anchor Skepticism**: Enhanced `AnchorEvaluator` to apply a 0.25x penalty (extra skepticism) to breakout score accumulation when high-SNR reflections are detected while physically stationary.
    - **State Propagation**: Expanded `SentinelResult`, `ProcessedLocation`, and `AlarmEvaluationState` to propagate the `isHighSnrReflection` flag through the engine layers.
- **Outcome**: 🟢 **PASSED**. The engine is now resilient against high-SNR urban reflections. False breakouts and geofence alerts in dense urban environments are suppressed via the adaptive latch. (SOT ID 765).

## ISSUE-OCT18.6-F1: Synthetic Physical Event Injection (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: The "Physical Gap" prevented remote validation of hardware-critical hardening logic (Chapters 3.2, 5.1, 15.1, 16.1). Manual testing required physical interaction with devices (shaking, shielding, unplugging) which is impossible in remote or automated CI environments.
- **Remediation**:
    - **Injection Framework**: Implemented `SyntheticEventInjector` as a central authority for hardware simulation.
    - **Hardware Layer Hooks**: Modified `HardwareSuite` to allow zeroing GNSS satellites and forcing high vibration floors (Shake simulation).
    - **Network Layer Hooks**: Updated `NetworkProvider` and `AndroidNetworkProvider` to intercept connectivity status and simulate interface loss.
    - **Integrity Integration**: Linked power tamper simulation to existing `IntegrityMonitor` power-event hooks.
    - **UI Control**: Integrated simulation toggles into the `DiagnosticsScreen` with real-time status tracking in `MainUiState`.
- **Outcome**: 🟢 **PASSED**. The validation gap is closed. Chapters 3.2, 5.1, 15.1, and 16.1 can now be validated remotely via the Diagnostics interface. (SOT ID 764).

## SIMP-OCT18.2-S1: Explicit Repository Lifecycle (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: Background tasks in `LogRepository` were automatically started in the constructor, making it difficult to control initialization order and environment detection (Test Mode).
- **Remediation**:
    - Refactored `LogRepository` to remove task initiation from `init`.
    - Added explicit `start()` method to `LogRepository`.
    - Added `start()` proxy to `LogManager`.
    - Integrated `logManager.start()` into `GpsApplication.onCreate()` to ensure tasks start only when the application is fully ready.
    - Improved Test Mode safety by checking `testModeProvider.isTestMode()` within the `start()` method.
- **Outcome**: 🟢 **PASSED**. Architectural clarity improved. Initialization is now predictable and safe for all environments. (SOT ID 763).

... [Remaining entries preserved] ...
