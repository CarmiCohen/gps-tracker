# Project Issues & Hardening Tracking (Rigorous Audit) - Oct16.1

## 🎯 Current Resumption Focus: Forensic Data Integrity & Scaling (SIMP-OCT15.21-S1).

## 🟢 Open Gaps & Unfinished Integration Points
*   *No active gaps.*

---

## 💡 Strategic Simplification Ideas (Ideas: 31)
*   **SIMP-OCT15.10-S1: Test Utility Decoupling (Low).** Many tests currently rely on `mock<MainViewModel>` even when testing pure mappers. Transitioning these to pure unit tests without ViewModel mocks would reduce test execution time and fragility.

---

## ✅ Resolved Traceability & Metadata Issues
*   **SIMP-OCT15.12-S1: Native Clamp Unification.** Resolved Oct16.1. Unified hardware-layer constraints (vibration floors, proximity debouncing) across JNI and JVM by introducing `EngineConstants.h`. Ported adaptive proximity logic to native fast-path for identical boundary behavior. Eliminated magic numbers and manual parity drift.
*   **SIMP-OCT15.21-S1: Immutable Ribbon Normals.** Resolved Oct14.1. Moved ribbon normalization logic from UI-thread buffers to the ingestion point in `TelemetryMapper`. Introduced `RibbonNormals` container in `ConnectionPoint` to ensure all history points carry pre-calculated, immutable norms. Eliminated redundant UI-thread calculations across 18 analytical ribbons.
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
- **Oct16.1: [SOT Count: 436 (Rules: 276), Open: H:0, M:0, L:0, Ideas: H:0, M:11, L:20, Testing: 17 (Sub-items: 127), QA: 337]**
- **Oct14.1: [SOT Count: 435 (Rules: 275), Open: H:0, M:0, L:0, Ideas: H:0, M:11, L:20, Testing: 17 (Sub-items: 127), QA: 337]**
