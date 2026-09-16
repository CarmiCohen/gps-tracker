# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Abstraction of the signaling pipeline to ensure deterministic testability of network lifecycles.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)
*   **Asynchronous Unregistration Race Condition in AndroidNetworkProvider (#20)**: In `AndroidNetworkProvider.kt`, `networkCallback.unregister(connectivityManager)` executes unregistration asynchronously on the main thread via `ManagedUnregistrationHelper.safeUnregister`. However, `isRegistered` is immediately set to `false` synchronously before the platform callback is detached. Rapid call sequences of `unregisterListener` and `registerListener` across threads can result in race conditions or redundant callback registrations.

---

## 🟢 Recently Evaluated Issues & Status
*   **Signaling Pipeline Hardening (#20)**: Enforced HTTP 2xx check for keep-alive probes in `ConnectivitySuite` to prevent premature failure counter resets during server-side errors (R-ID 349).
*   **Signaling Pipeline Abstraction (#20)**: Decoupled `ConnectivitySuite` from Android's `ConnectivityManager` and direct HTTP calls by introducing `NetworkProvider` and `SignalingTransport` interfaces. (R-ID 349).
*   **Legacy File Physical Deletion (#1057/1060)**: Verified physical removal of `A15PowerPolicy.kt`, `A15PowerPolicyProfileTest.kt`, and `A15PowerPolicyTest.kt`. Obsolete shells no longer exist in the filesystem.
*   **Logic Inconsistency & Broken Symmetry (#1060)**: Fully restored symmetry between `TrackerService` and `ViewerService`. Eliminated literal flags and ensured both roles use the unified `PerformanceTier` authority for hardware pokes and polling baselines (R-ID 348).
*   **Capability Consolidation (#1060)**: Successfully eliminated redundant properties (`isStaggeredTier`, `requiresAdaptationMuzzle`, `useStaggeredHydration`) from `HardwareCapabilities` and `PermissionState` (R-ID 348).
*   **PowerStateProvider Process Death Resilience (#1071)**: Validated that `FakePowerStateProvider` maintains consistency across component re-instantiation (R-ID 348).
*   **Test Suite Hardening (#1050/1052)**: Eliminated flaky shell-based Doze simulation in `ProductionReadinessAuditTest.kt` via `PowerStateProvider` abstraction (R-ID 348).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 349 (Rules: 70, IDs: 349), Resolved: 1073, Open: 1, Testing: 1 (Sub-items: 0), Ideas: 17, QA: 280]**
