# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.6

## 🎯 Current Resumption Focus: Performance telemetry and stability.

## ⚠️ Open Gaps & Unfinished Integration Points

---

## 💡 Strategic Simplification Ideas (Ideas: 18)
*   **SIMP-OCT13.21-S1: Composable Event Bubbling (Low).** Now that `ViewerScreen` and `TrackerScreen` only pass `onMainEvent`, we can consider moving remaining specialized lambdas into the `UiEvent` sealed class to further flatten the UI signatures.

---

## ✅ Resolved Traceability & Metadata Issues
*   **SIMP-OCT13.20-S1: Signaling Payload Conflation Logic.** Resolved Oct14.6. Unified conflation orchestration in SmartSignalingDispatcher.
*   **SIMP-OCT14.2-S1: Test Utility Unification.** Resolved Oct14.5. Unified `MockTimeProvider` into shared test-fixtures.
*   **ISSUE-OCT14.4-H1: S21FE Hardware Profile Verification.** Resolved Oct14.4. Verified project stability on S21FE hardware (`R5CRC14PG4F`).
*   **ISSUE-OCT14.3-H1: Full Hardening & Stress Audit.** Resolved Oct14.3. Verified project stability through 97 total tests (43 engine, 29 app unit, 25 instrumented).
*   **ISSUE-OCT14.2-H1: Hardening & Stress Test Restoration.** Resolved Oct14.1. Restored and verified the full test suite after signature regressions.
*   **SIMP-OCT13.30-S1: Activity Result Contracts Consolidation.** Resolved Oct14.1.
*   **ISSUE-OCT13.21-L2: Gradle 9.0 Compatibility Debt.** Resolved Oct14.1.
*   **ISSUE-OCT13.21-H1: Engine Compilation Failure (Regressions).** Resolved Oct14.1.

---

## 📊 Hardening Progress Dashboard
- **Oct14.6: [SOT Count: 408 (Rules: 254), Open: H:0, M:0, L:0, Ideas: H:0, M:10, L:8, Testing: 13 (Sub-items: 118), QA: 312]**
- **Oct14.5: [SOT Count: 407 (Rules: 253), Open: H:0, M:0, L:0, Ideas: H:0, M:10, L:9, Testing: 13 (Sub-items: 118), QA: 312]**
- **Oct14.4: [SOT Count: 406 (Rules: 252), Open: H:0, M:0, L:0, Ideas: H:0, M:10, L:10, Testing: 13 (Sub-items: 118), QA: 312]**
- **Oct14.3: [SOT Count: 406 (Rules: 252), Open: H:0, M:0, L:0, Ideas: H:0, M:10, L:10, Testing: 12 (Sub-items: 93), QA: 312]**
- **Oct14.1: [SOT Count: 405 (Rules: 252), Open: H:0, M:0, L:0, Ideas: H:0, M:10, L:10, Testing: 11 (Sub-items: 68), QA: 312]**
