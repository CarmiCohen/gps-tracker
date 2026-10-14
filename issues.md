# Project Issues & Hardening Tracking (Rigorous Audit) - Oct13.30

## 🎯 Current Resumption Focus: SIMP Refinement & Performance Audit.

## ⚠️ Open Gaps & Unfinished Integration Points

### 🔵 Low Priority (1)
*   **ISSUE-OCT13.21-L2: Gradle 9.0 Compatibility Debt.**
    *   Build uses deprecated features making it incompatible with Gradle 9.0. Flagged during `:app:assemble`.

---

## 💡 Strategic Simplification Ideas (Ideas: 20)
*   **SIMP-OCT13.30-S1: Activity Result Contracts Consolidation (Low).** Consider moving permission and file pickers into a more generic `SystemIntentHandler` to further decouple `MainActivity` from `MainAppContent`.
*   **SIMP-OCT13.21-S1: Composable Event Bubbling (Low).** Now that `ViewerScreen` and `TrackerScreen` only pass `onMainEvent`, we can consider moving remaining specialized lambdas into the `UiEvent` sealed class to further flatten the UI signatures.
*   **SIMP-OCT13.20-S1: Signaling Payload Conflation Logic (Low).** The conflation logic in `SmartSignalingDispatcher` for Maps vs Objects is very similar. Could be unified into a single generic strategy.
*   **SIMP-OCT13.8-S1: Revival-Trigger Consolidation (Low).** Consider merging `gpsRestartTrigger` and `revivalPulseJob` into a unified provider lifecycle manager.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT13.21-H1: Engine Compilation Failure (Regressions).** Resolved Oct13.30. Restored constants and fixed signatures.
*   **ISSUE-OCT13.21-M1: Integrity Task Parsing Error.** Resolved Oct13.30. Fixed `verifyProjectIntegrity` logic in root build.gradle.
*   **ISSUE-OCT13.21-L1: Remaining Dead Code Remnants (Engine).** Resolved Oct13.30. Pruned `isAdaptiveJump`, `qScale`, and `nowTs` from Engine and App.
*   **SIMP-OCT13.21-S2: Pure Event Architecture.** Resolved Oct13.30. Transitioned Activity-level launchers to `UiEffect` bus and pruned lambda injection in `MainAppContent`.
*   **ISSUE-OCT13.20-L1: Dead Code & Hygiene Cleanup.** Resolved Oct13.21 (Initial pass).
*   **ISSUE-OCT13.20-H1: Signaling Lifecycle Hardening.** Resolved Oct13.20.
*   **ISSUE-OCT13.20-M1: Manifest Audit.** Resolved Oct13.20.
*   **ISSUE-OCT13.20-M2: API Modernization.** Resolved Oct13.20.

---

## 📊 Hardening Progress Dashboard
- **Oct13.30: [SOT Count: 403, Open: H:0, M:0, L:1, Ideas: H:0, M:10, L:10, QA: 298]**
- **Oct13.21: [SOT Count: 403, Open: H:1, M:1, L:2, Ideas: H:0, M:10, L:11, QA: 298]**
- **Oct13.20: [SOT Count: 403, Open: H:0, M:0, L:1, Ideas: H:0, M:9, L:10, QA: 303]**
- **Oct10.3: [SOT Count: 405 (Rules: 251), Open: H:0, M:0, L:0, Ideas: H:0, M:9, L:9, Testing: 10 (Sub-items: 46), QA: 305]**
