# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalization of hardware schema consolidation and elimination of redundant behavioral branching.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### 1. Legacy File Physical Deletion (#1057/1060)
*   **Finding**: `A15PowerPolicy.kt`, `A15PowerPolicyProfileTest.kt`, and `A15PowerPolicyTest.kt` have been cleaned of active logic and marked with `@Deprecated`, but they still physically exist as empty shells.
*   **Still Needed**: Physical deletion of these files from the project repository to clean up technical debt and completely remove obsolete paths. (Tooling constraint: `git rm` unavailable).

### 2. Edge Cases and Environment Risks in Test Suite (#1050/1052)
*   **Finding**: `ProductionReadinessAuditTest.kt` utilizes `UiDevice.executeShellCommand("dumpsys deviceidle force-idle")` to simulate Doze behavior.
*   **Risks & Concerns**: 
    *   **Environment Flakiness**: Shell command execution requires signature-level permissions via UiAutomator, which causes flakiness or failures on unrooted physical devices or restricted emulator images in strict CI/CD environments.
    *   **Indirect Verification**: The test directly asserts `powerPolicy.shouldDeferSignaling()`, but doesn't verify the actual network backoff or deferred transmission side-effects within `ConnectivitySuite` under real forced-idle conditions.

---

## 🟢 Recently Evaluated Issues & Status
*   **Capability Consolidation & Symmetry (#1060)**: Successfully eliminated redundant properties (`isStaggeredTier`, `requiresAdaptationMuzzle`, `useStaggeredHydration`) from `HardwareCapabilities` and `PermissionState`. Background services, screens, and tests now inspect the `PerformanceTier` enum directly (R-ID 348).
*   **Traceability Synchronization (#1060)**: Completed a global sweep of header comments and metadata to synchronize `R-ID 347` (Unified Tier) references with `R-ID 348` (Consolidated Schema) across all core services, UI mappers, and buffers. Explicitly documented the progression from the unified tier to the consolidated schema in file headers.
*   **Service Symmetry Alignment (#1060)**: Refactored `ViewerService.kt` hardware poke gating to use the unified capability check and implemented the missing `onHeartbeat` callback, restoring full behavioral symmetry with `TrackerService.kt`.
*   **Audit Suite Refinement (#1050/1052)**: Integrated real-world saturation routines (CPU/IO burst) and implemented actual Doze state simulation via shell commands in `ProductionReadinessAuditTest.kt`.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 348 (Rules: 69, IDs: 348), Resolved: 1067, Open: 0, Testing: 1 (Sub-items: 0), Ideas: 18, QA: 279]**
