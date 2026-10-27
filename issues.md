git add .
git commit -m "Fix: Unit-Based Badge Scaling and Shared Component Refactoring. #ISSUE-OCT27.5-F1"
git tag -a Oct27.5 -m "Oct27.5 Release: UI Component Hardening"
git push origin Oct27.5

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct27.5

## 🎯 Current Resumption Focus: Forensic Integrity Baseline.

## 🔴 Open Gaps & Unfinished Integration Points
*   *None currently identified for the immediate forensic scope.*

---

## 💡 Strategic Simplification Ideas (Ideas: 99)
*   **SIMP-OCT27.5-S1: Badge Measurement Caching (Low).** Investigate if `MeasurableBadge` can benefit from `Modifier.layout` caching to further optimize rapid-fire HUD updates during high-pressure signaling.
*   **SIMP-OCT27.4-S1: Test Application Base Class (Medium).** Consolidate `GpsApplication` and `GpsTestBaseApplication` into a shared `BaseGpsApplication` to reduce duplication of WorkManager configuration logic.
*   **SIMP-OCT27.4-S2: Hilt Test Runner Optimization (Low).** Use `@CustomTestApplication` to generate a dedicated test application that natively handles Hilt Worker injection without manual `WorkManager.initialize` in `onCreate`.
*   **SIMP-OCT27.1-S1: Database Migration Automation (Medium).** Standardize migration generation to automatically handle index drops/creations for unique constraints to reduce manual `execSQL` boilerplate in `AppDatabase`.
*   **SIMP-OCT27.0-S1: Unit-Based Badge Scaling (Low).** Resolved Oct27.5. Transitioned HUD badges to use a shared `MeasurableBadge` component.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT27.5-F1: Unit-Based Badge Scaling.** Resolved Oct27.5. Implemented `MeasurableBadge` shared component and refactored HUD indicators to reduce redundant Compose declarations. (Resolved GAP-OCT27.4-1).
*   **ISSUE-OCT27.4-F1: PruningWorker Initialization Failure in Tests.** Resolved Oct27.0. Integrated `HiltWorkerFactory` into `GpsTestBaseApplication` and enforced explicit `WorkManager` initialization in the test environment.
*   **ISSUE-OCT27.4-F2: Signaling Conflation Audit Hardening.** Resolved Oct27.0. Hardened `ProductionReadinessAuditTest` with `ManualTestModeProvider` and connectivity assertions to ensure reliable conflation traces during high-pressure soak tests.
*   **ISSUE-OCT27.1-F1: Forensic Convergence Audit.** Resolved Oct27.0. Verified 100Hz burst sustainability and enforced persistence unique constraints to prevent data duplication.
*   **ISSUE-OCT27.0-F1: Signaling Conflation UI Feedback.** Resolved Oct27.0. Implemented numerical counter overlay for the `DAT` badge and integrated long-press diagnostic feedback to show exact conflation metrics.

---

## 📊 Hardening Progress Dashboard
- **Oct27.5: [SOT Count: 851 (Rules: 352), Open: H:0, M:0, L:0, Ideas: 99, Testing: 1 (Sub-items: 37), QA: PASSED]**
- **Oct27.0: [SOT Count: 850 (Rules: 351), Open: H:0, M:0, L:0, Ideas: 98, Testing: 1 (Sub-items: 36), QA: PASSED]**
- **Oct27.0: [SOT Count: 848 (Rules: 349), Open: H:0, M:0, L:0, Ideas: 96, Testing: 1 (Sub-items: 34), QA: PASSED]**
