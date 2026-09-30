# 🏛️ Resolution Archive - Sep.30.6

## 🏁 Issue #Audit-Sep.30.6: Field Soak & Stealth Validation
*   **Resolved**: Sep.30.6
*   **Root Cause**: Audit of forensic reliability and stealth enforcement (R872) for release readiness.
*   **Remediation**: 
    *   **Behavioral Audit**: Confirmed 65s PARKING hysteresis in `TrackerStateManager.kt` (60s hold + 5s buffer).
    *   **Stealth Audit**: Verified absolute local silence on Tracker hardware via `AudioSynthesizer` and `AppNotificationManager` suppression logic.
    *   **HUD Parity**: Confirmed that `MonitorService` tick authority eliminates velocity-state mismatches.
*   **Significance**: High (Release Integrity).
*   **SOT ID**: 563 (Forensic & Stealth Audit)

## 🏛️ Resolution Archive - Sep.30.6

## 🏁 Issue #1385: Peer Link & Diagnostic LED Stall
*   **Resolved**: Sep.30.6
*   **Root Cause**: Argument slot mismatch in Socket.io relay handlers. The `CommunicationManager` strictly assumed the payload was in the first argument (`args[0]`). Production relay servers often prepend a `routingId` (sender context), shifting the payload to `args[1]`. This caused parse errors (attempting to parse a String ID as JSON/Binary), resulting in red LEDs on the Viewer.
*   **Remediation**: 
    *   **Adaptive Extraction**: Refactored all relay handlers in `CommunicationManager.kt` (`handleLocationRelay`, `handleLocationRelayBinary`, etc.) to check `args.size` and dynamically extract the payload from either index 0 or 1.
*   **Significance**: High (Connectivity Integrity).
*   **SOT ID**: 561 (Peer Relay Argument Robustness)

## 🏁 Issue #1391: Alarm Leakage on Tracker (Stealth Violation)
*   **Resolved**: Sep.30.6
*   **Root Cause**: Unconditional UI promotion in `MainViewModel`. The `activeAlarmsFlow` observer reactively triggered `isRedScreenVisible = true` whenever any unresolved violation existed, regardless of whether the app was in Tracker or Viewer mode. This violated the stealth requirement (R872) for Trackers.
*   **Remediation**:
    *   **Stealth Guarding**: Added a strict `appMode == "viewer"` check to the reactive promotion logic in `MainViewModel.kt`.
*   **Significance**: High (Stealth & Behavioral Integrity).
*   **SOT ID**: 562 (Stealth Authority Enforcement)

## 🏁 Issue #1386: Tracker HUD Velocity State Inconsistency
*   **Resolved**: Sep.30.6
*   **Root Cause**: Distributed Behavioral Logic. `TrackerState` (MOVING vs PARKING) was being calculated independently in the UI mapper and the engine. The mapper used a simple 0.5 m/s gate, while the engine used `ACTIVE_MOVE_THRESHOLD` (2.0 m/s) plus a 60s moving-hold timer. This caused the HUD to show "MOVING" while the speed readout was "0.0 km/h".
*   **Remediation**:
    *   **Unified Authority**: Moved the definitive `TrackerState` calculation into the engine tick (`MonitorService.kt`) using `TrackerStateManager`. 
    *   **State Propagation**: Added `trackerState` to `SystemEvaluationSnapshot` to ensure persistence and signaling layers reflect the exact same state as the local HUD.
*   **Significance**: Medium (UX Consistency).
*   **SOT ID**: 560 (Centralized Behavioral Authority)

...
*(Full historical records maintained in SOT Archive)*