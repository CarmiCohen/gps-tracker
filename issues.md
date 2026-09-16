# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Abstraction of the signaling pipeline to ensure deterministic testability of network lifecycles.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

1. **Issue #1050/1052: High-Fidelity Doze Integration & Telemetry Gating Gap**
    *   **Finding**: Rigorous audit confirms that while `ProductionReadinessAuditTest.kt` is deterministic via fakes, the actual `AndroidPowerStateProvider` -> `PowerManager` integration is unverified.
    *   *Logic Inconsistency*: `ConnectivitySuite.startSyncLoop()`, `startIdentitySyncLoop()`, and `sendTelemetry()` currently lack checks for `shouldDeferSignaling()`. This allows telemetry flushes and identity syncs to bypass Doze restrictions when not in a violation state (Inconsistency with R-ID 338).
    *   *Still Needed*: 
        1. Implement a dedicated high-fidelity integration test (e.g., `PowerIntegrationAuditTest.kt`) using `UiDevice.executeShellCommand("dumpsys deviceidle force-idle")` to verify the actual bridge between the OS and the app.
        2. Patch `ConnectivitySuite.startSyncLoop()`, `startIdentitySyncLoop()`, and `sendTelemetry()` to respect the Doze deferral gate.
    *   *Requirement*: Verify that `shouldDeferSignaling` correctly detects the transition when the real `PowerManager` is queried, and ensure the system state is reliably restored (`unforce`) after test execution.
    *   *Side-Effect Mitigation*: Ensure that when a violation occurs, the sync loops immediately break out of the Doze deferral period rather than waiting for the next polling interval.
    *   *Risk*: Continued signaling during Doze (outside of violations) may lead to battery drain and platform-level process termination on Android 14/15.

---

## 🟢 Recently Evaluated Issues & Status

2. **Issue #1072: Static State Leakage in Test Fakes**
    *   *Status*: Resolved. Implemented a reset mechanism in `ProductionReadinessAuditTest.kt`'s `@Before` block to ensure `FakePowerStateProvider.isIdle` is cleared before every test case, ensuring test atomicity (Resolved Sep.16.12 / R-ID 350).

3. **Issue #1060: UI Sampling Refinement**
    *   *Status*: Confirmed that `MainViewModel.kt` has been fully transitioned to use direct `PerformanceTier` enum comparisons for all `.sample()` intervals, completely eliminating the legacy `useStaggeredHydration` boolean gating logic.

4. **Issue #20: AndroidNetworkProvider Race Condition**
    *   *Status*: Resolved race condition in asynchronous unregistration by serializing all platform state transitions on the Main Looper (Resolved Sep.16.11 / R-ID 349).

5. **Issue #20: Signaling Pipeline Hardening**
    *   *Status*: Enforced HTTP 2xx check for keep-alive probes in `ConnectivitySuite` to prevent premature failure counter resets during server-side errors (R-ID 349).

6. **Issue #20: Signaling Pipeline Abstraction**
    *   *Status*: Decoupled `ConnectivitySuite` from Android's `ConnectivityManager` and direct HTTP calls by introducing `NetworkProvider` and `SignalingTransport` interfaces (R-ID 349).

7. **Issue #1057/1060: Legacy File Physical Deletion**
    *   *Status*: Verified physical removal of `A15PowerPolicy.kt`, `A15PowerPolicyProfileTest.kt`, and `A15PowerPolicyTest.kt`. Obsolete shells no longer exist in the filesystem.

8. **Issue #1060: Logic Inconsistency & Broken Symmetry**
    *   *Status*: Fully restored symmetry between `TrackerService` and `ViewerService`. Eliminated literal flags and ensured both roles use the unified `PerformanceTier` authority for hardware pokes and polling baselines (R-ID 348).

9. **Issue #1060: Capability Consolidation**
    *   *Status*: Successfully eliminated redundant properties (`isStaggeredTier`, `requiresAdaptationMuzzle`, `useStaggeredHydration`) from `HardwareCapabilities` and `PermissionState` (R-ID 348).

10. **Issue #1071: PowerStateProvider Process Death Resilience**
    *   *Status*: Validated that `FakePowerStateProvider` maintains consistency across component re-instantiation (R-ID 348).

11. **Issue #1050/1052: Test Suite Logic Hardening**
    *   *Status*: Initial hardening achieved via abstraction to eliminate flakiness in logical tests; high-fidelity integration gaps moved to Open Gaps (Point #1).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 350 (Rules: 70, IDs: 350), Resolved: 1077, Open: 1, Testing: 1 (Sub-items: 0), Ideas: 17, QA: 280]**
