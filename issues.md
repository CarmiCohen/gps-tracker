git add .
git commit -m "Fix: PruningWorker Initialization and Signaling Audit Hardening. #ISSUE-OCT27.4-F1 #ISSUE-OCT27.4-F2"
git tag -a Oct27.4 -m "Oct27.4 Release: Test Infrastructure Hardening"
git push origin Oct27.4

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct27.4

## 🎯 Current Resumption Focus: Unit-Based Badge Scaling.

## 🔴 Open Gaps & Unfinished Integration Points
*   *None currently identified for the immediate forensic scope.*

---

## 💡 Strategic Simplification Ideas (Ideas: 98)
*   **SIMP-OCT27.4-S1: Test Application Base Class (Medium).** Consolidate `GpsApplication` and `GpsTestBaseApplication` into a shared `BaseGpsApplication` to reduce duplication of WorkManager configuration logic.
*   **SIMP-OCT27.4-S2: Hilt Test Runner Optimization (Low).** Use `@CustomTestApplication` to generate a dedicated test application that natively handles Hilt Worker injection without manual `WorkManager.initialize` in `onCreate`.
*   **SIMP-OCT27.1-S1: Database Migration Automation (Medium).** Standardize migration generation to automatically handle index drops/creations for unique constraints to reduce manual `execSQL` boilerplate in `AppDatabase`.
*   **SIMP-OCT27.0-S1: Unit-Based Badge Scaling (Low).** Transition HUD badges to use a shared `MeasurableBadge` component to reduce redundant `Box` and `Text` declarations in `SharedUiComponents.kt`.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT27.4-F1: PruningWorker Initialization Failure in Tests.** Resolved Oct27.4. Integrated `HiltWorkerFactory` into `GpsTestBaseApplication` and enforced explicit `WorkManager` initialization in the test environment.
*   **ISSUE-OCT27.4-F2: Signaling Conflation Audit Hardening.** Resolved Oct27.4. Hardened `ProductionReadinessAuditTest` with `ManualTestModeProvider` and connectivity assertions to ensure reliable conflation traces during high-pressure soak tests.
*   **ISSUE-OCT27.1-F1: Forensic Convergence Audit.** Resolved Oct27.1. Verified 100Hz burst sustainability and enforced persistence unique constraints to prevent data duplication. (Resolved GAP-OCT27.0-1).
*   **ISSUE-OCT27.0-F1: Signaling Conflation UI Feedback.** Resolved Oct27.0. Implemented numerical counter overlay for the `DAT` badge and integrated long-press diagnostic feedback to show exact conflation metrics. (Resolved GAP-OCT26.5-1).

---

## 📊 Hardening Progress Dashboard
- **Oct27.4: [SOT Count: 850 (Rules: 351), Open: H:0, M:0, L:0, Ideas: 98, Testing: 1 (Sub-items: 36), QA: PASSED]**
- **Oct27.1: [SOT Count: 848 (Rules: 349), Open: H:0, M:0, L:0, Ideas: 96, Testing: 1 (Sub-items: 34), QA: PASSED]**
- **Oct27.0: [SOT Count: 847 (Rules: 348), Open: H:0, M:1, L:0, Ideas: 95, Testing: 1 (Sub-items: 33), QA: PASSED]**
