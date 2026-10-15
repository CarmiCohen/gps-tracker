# Project Issues & Hardening Tracking (Rigorous Audit) - Oct15.11

## 🎯 Current Resumption Focus: Unit Test Integrity & Manual UI Hardening (ISSUE-OCT15.10-F1..F6).

## 🟢 Open Gaps & Unfinished Integration Points
*   *No active gaps. ISSUE-OCT15.10-F2 resolved in Oct15.11.*

---

## 💡 Strategic Simplification Ideas (Ideas: 30)
*   **SIMP-OCT15.10-S2: Diagnostic HUD Performance (Low).** The global status bar contains many animated elements. During Mali anomalies, we should consider freezing animations to further reduce UI thread pressure.
*   **SIMP-OCT15.10-S1: Test Utility Decoupling (Low).** Many tests currently rely on `mock<MainViewModel>` even when testing pure mappers. Transitioning these to pure unit tests without ViewModel mocks would reduce test execution time and fragility.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT15.10-F2: Proximity Debounce Overflow.** Resolved Oct15.11. Fixed assignment typo in `HardwareSuite.kt`, implemented JNI success checks, and added safety clamping in `TelemetryMapper` to prevent invalid timestamp propagation.
*   **ISSUE-OCT15.10-F6: Mode Restoration Logic.** Resolved Oct15.10. Implemented automatic `StartService` emission in `MainViewModel` init block if a prior `appMode` is detected.
*   **ISSUE-OCT15.10-F5: Tracker Geofence Restriction.** Resolved Oct15.10. Guarded `AddHomePoint`, `RemoveHomePoint`, and `ClearHomePoints` in `UiEventCoordinator` to enforce "Viewer-Only" requirement.
*   **ISSUE-OCT15.10-F4: HUD Rendering Stalled.** Resolved Oct15.10. Integrated `PhoneSetupOverlay` into `OverlayHost` in `MainAppContent.kt`.
*   **ISSUE-OCT15.10-F3: Engine Version String Mismatch.** Resolved Oct15.10. Updated `DashboardMapper.kt` and `MainAppContent.kt` to reflect `Oct15.10`.
*   **ISSUE-OCT15.10-F1: Unit Test Suite Restoration.** Resolved Oct15.10. Updated `DashboardContinuityTest`, `MainViewModelMappingTest`, and `UxLogicFormattingTest` to align with the Pure Mapping Architecture (SIMP-OCT14.21-S1). Restored 100% unit test pass rate.

---

## 📊 Hardening Progress Dashboard
- **Oct15.11: [SOT Count: 432 (Rules: 271), Open: H:0, M:0, L:0, Ideas: H:0, M:11, L:8, Testing: 16 (Sub-items: 125), QA: 333]**
- **Oct15.10: [SOT Count: 431 (Rules: 270), Open: H:0, M:1, L:0, Ideas: H:0, M:11, L:8, Testing: 16 (Sub-items: 125), QA: 333]**
