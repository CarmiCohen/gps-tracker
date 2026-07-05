# Forensic Handover - v8.9.94 (A15 & G990E Stability Hardening)

## 📌 Status: Stable / Build PASS / Hardware Hardened
This cycle completes the deep remediation of hardware-specific instabilities on Samsung A15 and S21 FE (G990E) devices.

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
*   **Version Increment**: Bumping to **v8.9.94**.
*   **Documentation Sync**: All 3 open technical issues marked as resolved in `issues.md`.

### 🟡 Pending Validation
*   **A15 Jitter Verification**: Confirm state stability on A15 Tracker under clear sky vs. indoor transition.
*   **G990E Display Muzzle**: Verify Viewer telemetry remains silent during G990E AOD transitions.
*   **Adaptation Settling**: Monitor logcat for "Settling A15 Polling..." messages during movement start.

### 🛠 Instructions for Resumption
1.  **Environment**: Connect Samsung A15 (Tracker) and G990E (Viewer).
2.  **Verification**: 
    *   Deploy **v8.9.94**.
    *   Audit Logcat for "Issue #037" and "Settling A15 Polling" identifiers.
    *   Verify the dashboard no longer flickers between states during stationary monitoring.
