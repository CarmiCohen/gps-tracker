# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalization of hardware schema consolidation and elimination of redundant behavioral branching.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### 2. Documentation & Traceability Anomalies
*   **Finding**: Multiple files (UI mappers, monitors, and utility classes) still reference `R-ID 347` in historical comments without explicitly linking to the consolidated `R-ID 348` authority.
*   **Still Needed**: A final sweep of header comments to explicitly specify the progression from `R-ID 347` to `R-ID 348` across all affected components to avoid audit confusion.

---

## 🟢 Recently Evaluated Issues & Status
*   **Edge Cases and Environment Risks in Test Suite (#1050/1052)**: Eliminated flaky shell-based Doze simulation in `ProductionReadinessAuditTest.kt` by introducing `PowerStateProvider`. The test suite now uses a deterministic Hilt-injected `FakePowerStateProvider`, ensuring stability across all CI/CD environments (R-ID 348).
*   **Legacy File Physical Deletion (#1057/1060)**: Physically deleted `A15PowerPolicy.kt`, `A15PowerPolicyProfileTest.kt`, and `A15PowerPolicyTest.kt` from the repository, completely removing obsolete paths and closing out legacy technical debt.
*   **Capability Consolidation & Symmetry (#1060)**: Successfully eliminated redundant properties (`isStaggeredTier`, `requiresAdaptationMuzzle`, `useStaggeredHydration`) from `HardwareCapabilities` and `PermissionState`. Background services, screens, and tests now inspect the `PerformanceTier` enum directly (R-ID 348).
*   **Service Symmetry Alignment (#1060)**: Verified that both `TrackerService.kt` and `ViewerService.kt` have transitioned from hardcoded literals to unified capability checks for hardware poking.
*   **Metadata Synchronization (#1052)**: Corrected legacy header references in `ProductionReadinessAuditTest.kt` and ensured background services maintain a clear historical trace for `R-ID 348`.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 348 (Rules: 69, IDs: 348), Resolved: 1069, Open: 1, Testing: 1 (Sub-items: 0), Ideas: 18, QA: 279]**
