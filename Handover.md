# Forensic Handover (Sep.30.40 - BEHAVIORAL UNITY)

## 🎯 Current System State
*   **Version**: `Sep.30.40` | **Status**: BEHAVIORAL AUTHORITY UNIFIED, PEER LINK RESTORED, STEALTH ENFORCED.
*   **Peer Link Robustness (#1385)**:
    *   **Remediation**: Refactored `CommunicationManager.kt` (lines 224-278). All relay event handlers (`handleLocationRelay`, `handleLocationRelayBinary`, `handleLogRelay`, etc.) now adaptively extract payloads by checking `args.size > 1` to accommodate relay servers that prepend a `routingId`.
    *   **Verification**: Verified TRK/DAT/VWR LEDs return to Green/Active upon receiving relayed telemetry.
*   **Tracker Stealth Authority (#1391)**:
    *   **Remediation**: In `MainViewModel.kt` (lines 261 & 363), guarded Reactive Red-Screen promotion and siren engagement triggers with a strict `_uiState.value.session.appMode == "viewer"` check. Trackers now remain dark and silent during violations as per **R872**.
*   **Behavioral Authority Unified (#1386)**:
    *   **Remediation**: 
        1.  Added `trackerState: TrackerState` to `SystemEvaluationSnapshot` in `EngineModels.kt`.
        2.  Updated `MonitorService.kt` (line 512) to calculate definitively the `TrackerState` using `TrackerStateManager.updateState` during the primary engine tick.
        3.  Refactored `TelemetryMapper.kt` to pull `trackerState` directly from snapshots or protos, eliminating distributed logic and HUD/Signaling velocity mismatches (R-ID 548).
*   **Rule Enforcement**: Added **Rule 1.63 (Centralized Behavioral Authority)** and **Rule 1.64 (Peer Relay Argument Robustness)** to `SOT_MASTER_REQUIREMENTS.md`.

## 🚀 Resumption Focus: Field Soak Validation
*   **Target**: Sustained forensic probe reliability on Samsung A15 hardware.
*   **Symptoms to Monitor**: Verify that "MOVING" vs "PARKING" states in the HUD are perfectly consistent with the 0.0 km/h readout (accounting for the 60s moving-hold timer).
*   **Audit Path**:
    1.  Perform long-duration stationary test to verify "PARKING" transition.
    2.  Check `StatusBar` for accidental "Ghost Alarms" on Tracker device.

---

## 🛡️ Core Architecture Blueprint
1.  **Centralized Authority**: High-level behavioral states MUST be determined by the engine tick, not the mapping/serialization layers.
2.  **Argument Robustness**: Signaling consumers MUST be tolerant of `routingId` presence in Socket.io payloads.
3.  **Stealth First**: Tracker mode MUST suppress all local UI alarms and sirens, delegating responsibility to the Viewer.

---

## 📊 Hardening Progress Dashboard (Sep.30.40)
- **Status**: [SOT Count: 224 (Rules: 74), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:2, Testing: 6 (Sub-items: 29), QA: 304]
- **QA Record**: Fully synchronized to baseline `Sep.30.40`.
