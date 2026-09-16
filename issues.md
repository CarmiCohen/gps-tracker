# Project Issues & Hardening Tracking (Sep.16.03)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalization of hardware schema consolidation and elimination of redundant behavioral branching.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Audit Suite Refinement (#1050/1052)**: `ProductionReadinessAuditTest.kt` lacks actual Doze state simulation and the stress test is purely logical; lacks integration with the real-world saturation routines found in `TrackerService`.
*   **Metadata Inconsistency (#1052)**: `ProductionReadinessAuditTest` header references R-ID 343 (instead of 344) and version `Sep.15.14` (instead of `Sep.15.15`).

## 🟢 Recently Resolved Issues (Sep.16.03)
*   **Legacy Cleanup (#1057/1060)**: Logically removed and deprecated obsolete `A15PowerPolicy`, `A15PowerPolicyProfileTest`, and `A15PowerPolicyTest` components. These have been fully superseded by the `UnifiedPowerPolicy` framework. (R-ID 348).
*   **Metadata Synchronization (#1060)**: Synchronized header comments in `MainViewModel`, `MainUiState`, `TrackerService`, `ViewerService`, and `SystemStatusProvider` to correctly reference R-ID 348.
*   **UI Refresh Optimization (#1060)**: Transitioned `MainViewModel` sampling logic from `useStaggeredHydration` to direct `PerformanceTier` enum comparison for architectural consistency. (R-ID 348).
*   **Logic Hardening (#1060)**: Updated `TrackerService.kt` to use the consolidated `capabilities.isStaggeredTier` flag for hardware pokes instead of hardcoded logic. (R-ID 348).
*   **Hardware Capability Consolidation (#1060)**: Merged redundant performance flags (`isStaggeredTier`, `requiresAdaptationMuzzle`, `useStaggeredHydration`) into a unified `PerformanceTier` enum. (R-ID 348). (Sep.16.02).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 348 (Rules: 69, IDs: 348), Resolved: 1064, Open: 2, Testing: 1, Ideas: 18, QA: 279]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (Sep.16.03)*
