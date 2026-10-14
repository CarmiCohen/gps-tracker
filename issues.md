# Project Issues & Hardening Tracking (Rigorous Audit) - Oct13.21

## 🎯 Current Resumption Focus: Build Restoration & Engine Fixes.

## ⚠️ Open Gaps & Unfinished Integration Points

### 🔴 High Priority (1)
*   **ISSUE-OCT13.21-H1: Engine Compilation Failure (Regressions).** 
    *   `MainAlarmLogic.kt`: Unresolved reference `JUMP_HOLD_DURATION_MS` and `compareTo` ambiguity at line 406.
    *   `TelemetryUtils.kt`: Unresolved reference `GPS_INDEX_AGE_SCALING`.
    *   *Root Cause*: Over-aggressive constant pruning in `EngineConstants.kt` during Oct13.21 hygiene pass. Logic still depends on these values.

### 🟡 Medium Priority (1)
*   **ISSUE-OCT13.21-M1: Integrity Task Parsing Error.**
    *   `verifyProjectIntegrity` fallback triggered by malformed version: `For input string: "INVALID"`.
    *   *Root Cause*: Task failed to parse `versionName` from project configuration, likely due to a format mismatch after the recent version increment.

### 🔵 Low Priority (2)
*   **ISSUE-OCT13.21-L1: Remaining Dead Code Remnants (Engine).**
    *   `EngineModels.kt:796`: Parameter `isAdaptiveJump` is never used.
    *   `LocationSentinel.kt:61`: Parameter `qScale` is never used.
    *   `LocationSentinel.kt:201`: Parameter `nowTs` is never used.
*   **ISSUE-OCT13.21-L2: Gradle 9.0 Compatibility Debt.**
    *   Build uses deprecated features making it incompatible with Gradle 9.0. Flagged during `:app:assemble`.

---

## 💡 Strategic Simplification Ideas (Ideas: 21)
*   **SIMP-OCT13.21-S2: Pure Event Architecture (Medium).** Complete the transition of all Activity-level interactions (permissions, file picking) to the `UiEvent` bus to remove remaining prop-drilling.
*   **SIMP-OCT13.21-S1: Composable Event Bubbling (Low).** Now that `ViewerScreen` and `TrackerScreen` only pass `onMainEvent`, we can consider moving remaining specialized lambdas into the `UiEvent` sealed class to further flatten the UI signatures.
*   **SIMP-OCT13.20-S1: Signaling Payload Conflation Logic (Low).** The conflation logic in `SmartSignalingDispatcher` for Maps vs Objects is very similar. Could be unified into a single generic strategy.
*   **SIMP-OCT13.8-S1: Revival-Trigger Consolidation (Low).** Consider merging `gpsRestartTrigger` and `revivalPulseJob` into a unified provider lifecycle manager.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT13.20-L1: Dead Code & Hygiene Cleanup.** Resolved Oct13.21 (Initial pass).
*   **ISSUE-OCT13.20-H1: Signaling Lifecycle Hardening.** Resolved Oct13.20.
*   **ISSUE-OCT13.20-M1: Manifest Audit.** Resolved Oct13.20.
*   **ISSUE-OCT13.20-M2: API Modernization.** Resolved Oct13.20.

---

## 📊 Hardening Progress Dashboard
- **Oct10.3: [SOT Count: 403, Open: H:1, M:1, L:2, Ideas: H:0, M:10, L:11, QA: 298]**
- **Oct13.20: [SOT Count: 403, Open: H:0, M:0, L:1, Ideas: H:0, M:9, L:10, QA: 303]**
- **Oct10.3: [SOT Count: 405 (Rules: 251), Open: H:0, M:0, L:0, Ideas: H:0, M:9, L:9, Testing: 10 (Sub-items: 46), QA: 305]**
