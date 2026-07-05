# Forensic Handover - v8.9.95 (UI Refresh Consistency)

## 📌 Status: Stable / Build PASS / UI Hardened
This cycle addresses UI refresh consistency for forensic fields on the dashboard.

### 🟢 Completed: Issue #032 (UI Refresh Consistency)
*   **Staleness Gate**: Implemented `isForensicFresh` logic in `DashboardUseCase`.
*   **Threshold Alignment**: Synced forensic field visibility with `WATCH_DOG_UI_GRACE_MS` (15s).
*   **Field Hardening**: `Prox Debounce`, `Rolling Vibe`, and `Chair Forensics` now correctly revert to "--" when telemetry data exceeds the 15s staleness threshold, preventing misleading data display during network or sensor gaps.

### 🟢 Completed: Issue #036 (A15 Jitter Hardening)
*   **Hardened Gates**: Introduced `JUMP_GATE_SENSOR_MISMATCH_A15_MPS` (5.0 m/s) and `JUMP_GATE_VISUAL_JITTER_A15_METERS` (25m).
*   **Jump Engine Sync**: Updated `PhysicsUtils.isVisualJump` to filter raw A15 sensor noise using these specific gates.

### 🟢 Completed: Issue #037 (Display State Hardening)
*   **Flicker Detection**: Added `DisplayListener` to `AppSensorManager` to detect rapid ON/DOZE toggling (< 1000ms) on G990E devices.
*   **Proximity Muzzle**: Suppressed virtual proximity "Far" transitions during Samsung AOD state spam to prevent telemetry noise.

### 🟢 Completed: Issue #038 (Adaptation Stability)
*   **Settling Window**: Added `ADAPTATION_SETTLING_MS` (5000ms).
*   **Adaptation Muzzle**: Implemented logic in `TrackerService` and `LocationProcessor` to increase filter skepticism during GPS polling transitions on A15.

### 🟢 Completed: Infrastructure & Deployment
*   **Version Increment**: Bumping to **v8.9.95**.
*   **Documentation Sync**: Issue #032 marked as resolved in `issues.md`.

### 🟡 Pending Validation
*   **Forensic Staleness**: Verify that `Prox Debounce` and `Rolling Vibe` clear within exactly 15s of tracker disconnection.
*   **A15 Jitter Verification**: Confirm state stability on A15 Tracker under clear sky vs. indoor transition.
*   **G990E Display Muzzle**: Verify Viewer telemetry remains silent during G990E AOD transitions.
*   **Adaptation Settling**: Monitor logcat for "Settling A15 Polling..." messages during movement start.

### 🛠 Instructions for Resumption
1.  **Environment**: Connect Samsung A15 (Tracker) and G990E (Viewer).
2.  **Verification**: 
    *   Deploy **v8.9.95**.
    *   Disconnect the tracker and verify forensic fields clear after 15 seconds.
    *   Verify the dashboard no longer flickers between states during stationary monitoring.
