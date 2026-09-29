# 🏛️ Resolution Archive - Sep.29.30

## 🏁 Issue #1380: Peer Link Discovery & Navigation Hardening
*   **Resolved**: Sep.29.30
*   **Root Cause**: 
    1.  **Handshake Asymmetry**: The Tracker logic was configured to only send telemetry packets once a GPS fix was obtained. In indoors or poor-signal environments, the Tracker would remain silent, preventing the Viewer from discovering it even if the Relay was active.
    2.  **UI Navigation Deadlock**: The `SettingsOverlay` (Composable) did not consume the `NavigateToDiagnostics` event by closing itself. It remained as a full-screen overlay, occluding the `DiagnosticsScreen` and making the "Diagnostics" button appear unresponsive.
*   **Remediation**:
    *   **Bypass Heartbeat**: Injected a "Bypass Heartbeat" into `MonitorService.onHeartbeat()` (30s interval). The Tracker now transmits a telemetry pulse to the Relay immediately upon session start, regardless of GPS availability.
    *   **Overlay Cleanup**: Modified `SettingsComponents.kt` to explicitly call `onEvent(UiEvent.ToggleSettings(false))` before navigating to Diagnostics, ensuring a clean UI transition.
*   **Significance**: High (Connectivity Reliability & UX Integrity).
*   **SOT ID**: 552 (Bypass Heartbeat & Navigation Cleanliness)

## 🏁 Issue #1378: Cross-Test State Leakage & Buffer Validation Fix
*   **Resolved**: Sep.29.3
*   **Root Cause**: Identical symptoms of forensic probe disappearance on budget (A15) and high-performance (S21) hardware persisted during instrumented test suites because of cross-test state leakage. `ForensicSpillBuffer` is a Singleton, and when sequential tests like `verifyExtendedSoakSimulation` and `verifySignalingLifecycleProbes` ran, they shared the underlying state. The test suite's `hasPending()` checks and buffer drains created race conditions with the internal buffer pointers.
*   **Remediation**:
    *   **Strict Isolation**: Implemented `resetBufferForTest()` directly interfacing with internal schema pointers across all related test `@Before` hooks.
    *   **Persistence Hardening**: Hardcoded loop clearing procedures to avoid potential `Arrays.fill` off-by-one errors for the 128-byte chunk alignment.
    *   **Validation**: Built `ForensicBufferSchemaTest` to exhaustively test byte boundaries. Successfully passed 23/23 tests natively on both A15 and S21 devices.
*   **Significance**: Critical (Test Architecture Reliability).
*   **SOT ID**: 551 (Test Buffer Isolation)

...
*(Full historical records maintained in SOT Archive)*