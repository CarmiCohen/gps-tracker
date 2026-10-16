# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.1

## 🎯 Current Resumption Focus: Unit Test Integrity & Manual UI Hardening (ISSUE-OCT15.10-F1..F6).

## 🟢 Open Gaps & Unfinished Integration Points
*   *No active gaps. SIMP-OCT15.20-S1 resolved in Oct14.1.*

---

## 💡 Strategic Simplification Ideas (Ideas: 32)
*   **SIMP-OCT15.21-S1: Immutable Ribbon Normals (Low).** Ribbon normalization values are currently calculated in the mapper. Moving these to the ingestion point in `TelemetryMapper` and making the arrays strictly immutable in `HistoryBuffer` would prevent any potential race conditions during concurrent rendering.
*   **SIMP-OCT15.12-S1: Native Clamp Unification (Medium).** We now clamp proximity and vibration floors in both JNI and JVM. Moving these hard-coded limits to the shared `VibrationBatch` and `ProximityBatch` headers (if they were C++ headers) would be ideal, but for now, the `EngineConstants.kt` parity is the source of truth.
*   **SIMP-OCT15.10-S1: Test Utility Decoupling (Low).** Many tests currently rely on `mock<MainViewModel>` even when testing pure mappers. Transitioning these to pure unit tests without ViewModel mocks would reduce test execution time and fragility.

---

## ✅ Resolved Traceability & Metadata Issues
*   **SIMP-OCT15.20-S1: Animation Controller Composition.** Resolved Oct14.1. Centralized Mali animation gating using `LocalAnimationGating` and specialized Composable wrappers (`gatedAnimateFloatAsState`, `gatedInfinitePulse`). Eliminated redundant parameter propagation across 12 UI components and ensured architectural consistency.
*   **ISSUE-OCT15.20-F1: HUD Animation Gating (Mali Hardening).** Resolved Oct14.1. Systematically gated non-critical UI animations (Speed, Battery, Distance, Moving Status, and System Alert Pulse) based on `isMaliAnomaly` health state. Updated `HeaderBar`, `DashboardHeader`, and `StatusBar` to freeze animations into static states during driver stress to reduce UI thread pressure.
*   **ISSUE-OCT15.11-F1: Vibration Floor Jitter.** Resolved Oct14.1. Implemented boundary clamping `[0.02, 2.0]` G in `jdhardware-jni.cpp` (n13, n19), `SentinelValidator.kt` (JVM fallback), and `TelemetryMapper.kt` (ingestion/mapping). Added `VIBRATION_FLOOR_MIN/MAX` to `EngineConstants.kt`.
*   **ISSUE-OCT15.10-F2: Proximity Debounce Overflow.** Resolved Oct14.1. Fixed assignment typo in `HardwareSuite.kt`, implemented JNI success checks, and added safety clamping in `TelemetryMapper` to prevent invalid timestamp propagation.
*   **ISSUE-OCT15.10-F6: Mode Restoration Logic.** Resolved Oct14.1. Implemented automatic `StartService` emission in `MainViewModel` init block if a prior `appMode` is detected.
*   **ISSUE-OCT15.10-F5: Tracker Geofence Restriction.** Resolved Oct14.1. Guarded `AddHomePoint`, `RemoveHomePoint`, and `ClearHomePoints` in `UiEventCoordinator` to enforce "Viewer-Only" requirement.
*   **ISSUE-OCT15.10-F4: HUD Rendering Stalled.** Resolved Oct14.1. Integrated `PhoneSetupOverlay` into `OverlayHost` in `MainAppContent.kt`.
*   **ISSUE-OCT15.10-F3: Engine Version String Mismatch.** Resolved Oct14.1. Updated `DashboardMapper.kt` and `MainAppContent.kt` to reflect `Oct14.1`.
*   **ISSUE-OCT15.10-F1: Unit Test Suite Restoration.** Resolved Oct14.1. Updated `DashboardContinuityTest`, `MainViewModelMappingTest`, and `UxLogicFormattingTest` to align with the Pure Mapping Architecture (SIMP-OCT14.21-S1). Restored 100% unit test pass rate.

---

## 📊 Hardening Progress Dashboard
- **Oct14.1: [SOT Count: 435 (Rules: 274), Open: H:0, M:0, L:0, Ideas: H:0, M:12, L:20, Testing: 17 (Sub-items: 127), QA: 336]**
- **Oct14.1: [SOT Count: 434 (Rules: 273), Open: H:0, M:0, L:0, Ideas: H:1, M:13, L:18, Testing: 16 (Sub-items: 126), QA: 335]**
