# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.1

## 🎯 Current Resumption Focus: Unit Test Integrity & Manual UI Hardening (ISSUE-OCT15.10-F1..F6).

## 🟢 Open Gaps & Unfinished Integration Points
*   *No active gaps. ISSUE-OCT15.11-F1 resolved in Oct14.1.*

---

## 💡 Strategic Simplification Ideas (Ideas: 31)
*   **SIMP-OCT15.12-S1: Native Clamp Unification (Medium).** We now clamp proximity and vibration floors in both JNI and JVM. Moving these hard-coded limits to the shared `VibrationBatch` and `ProximityBatch` headers (if they were C++ headers) would be ideal, but for now, the `EngineConstants.kt` parity is the source of truth.
*   **SIMP-OCT15.10-S2: Diagnostic HUD Performance (Low).** The global status bar contains many animated elements. During Mali anomalies, we should consider freezing animations to further reduce UI thread pressure.
*   **SIMP-OCT15.10-S1: Test Utility Decoupling (Low).** Many tests currently rely on `mock<MainViewModel>` even when testing pure mappers. Transitioning these to pure unit tests without ViewModel mocks would reduce test execution time and fragility.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT15.11-F1: Vibration Floor Jitter.** Resolved Oct14.1. Implemented boundary clamping `[0.02, 2.0]` G in `jdhardware-jni.cpp` (n13, n19), `SentinelValidator.kt` (JVM fallback), and `TelemetryMapper.kt` (ingestion/mapping). Added `VIBRATION_FLOOR_MIN/MAX` to `EngineConstants.kt`.
*   **ISSUE-OCT15.10-F2: Proximity Debounce Overflow.** Resolved Oct14.1. Fixed assignment typo in `HardwareSuite.kt`, implemented JNI success checks, and added safety clamping in `TelemetryMapper` to prevent invalid timestamp propagation.
*   **ISSUE-OCT15.10-F6: Mode Restoration Logic.** Resolved Oct14.1. Implemented automatic `StartService` emission in `MainViewModel` init block if a prior `appMode` is detected.
*   **ISSUE-OCT15.10-F5: Tracker Geofence Restriction.** Resolved Oct14.1. Guarded `AddHomePoint`, `RemoveHomePoint`, and `ClearHomePoints` in `UiEventCoordinator` to enforce "Viewer-Only" requirement.
*   **ISSUE-OCT15.10-F4: HUD Rendering Stalled.** Resolved Oct14.1. Integrated `PhoneSetupOverlay` into `OverlayHost` in `MainAppContent.kt`.
*   **ISSUE-OCT15.10-F3: Engine Version String Mismatch.** Resolved Oct14.1. Updated `DashboardMapper.kt` and `MainAppContent.kt` to reflect `Oct14.1`.
*   **ISSUE-OCT15.10-F1: Unit Test Suite Restoration.** Resolved Oct14.1. Updated `DashboardContinuityTest`, `MainViewModelMappingTest`, and `UxLogicFormattingTest` to align with the Pure Mapping Architecture (SIMP-OCT14.21-S1). Restored 100% unit test pass rate.

---

## 📊 Hardening Progress Dashboard
- **Oct14.1: [SOT Count: 433 (Rules: 272), Open: H:0, M:0, L:0, Ideas: H:0, M:12, L:8, Testing: 16 (Sub-items: 125), QA: 334]**
- **Oct14.1: [SOT Count: 432 (Rules: 271), Open: H:0, M:0, L:0, Ideas: H:0, M:11, L:8, Testing: 16 (Sub-items: 125), QA: 333]**
- **Oct14.1: [SOT Count: 431 (Rules: 270), Open: H:0, M:1, L:0, Ideas: H:0, M:11, L:8, Testing: 16 (Sub-items: 125), QA: 333]**
