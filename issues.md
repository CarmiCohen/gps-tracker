# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Abstraction of the signaling pipeline to ensure deterministic testability of network lifecycles.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

---

## 🟢 Recently Evaluated Issues & Status
*   **Signaling Pipeline Abstraction (#20)**: Decoupled `ConnectivitySuite` from Android's `ConnectivityManager` and direct HTTP calls by introducing `NetworkProvider` and `SignalingTransport` interfaces. This enables deterministic testing of network handovers and out-of-band keep-alive logic without system side effects (R-ID 349).
*   **PowerStateProvider Process Death Resilience (#1071)**: Validated that the Hilt-injected `PowerStateProvider` (via `FakePowerStateProvider` with static state) maintains consistency across component re-instantiation, ensuring no telemetry gaps during simulated process death (R-ID 348).
*   **Documentation & Traceability Hardening (#1060)**: Completed a comprehensive sweep of header comments across 20+ components. Explicitly linked all historical references of `R-ID 347` to the consolidated `R-ID 348` authority, ensuring architectural continuity and audit clarity (R-ID 348).
*   **Edge Cases and Environment Risks in Test Suite (#1050/1052)**: Eliminated flaky shell-based Doze simulation in `ProductionReadinessAuditTest.kt` by introducing `PowerStateProvider`. The test suite now uses a deterministic Hilt-injected `FakePowerStateProvider`, ensuring stability across all CI/CD environments (R-ID 348).
*   **Legacy File Physical Deletion (#1057/1060)**: Physically deleted `A15PowerPolicy.kt`, `A15PowerPolicyProfileTest.kt`, and `A15PowerPolicyTest.kt` from the repository, completely removing obsolete paths and closing out legacy technical debt.
*   **Capability Consolidation & Symmetry (#1060)**: Successfully eliminated redundant properties (`isStaggeredTier`, `requiresAdaptationMuzzle`, `useStaggeredHydration`) from `HardwareCapabilities` and `PermissionState`. Background services, screens, and tests now inspect the `PerformanceTier` enum directly (R-ID 348).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 349 (Rules: 70, IDs: 349), Resolved: 1072, Open: 0, Testing: 1 (Sub-items: 0), Ideas: 17, QA: 280]**
