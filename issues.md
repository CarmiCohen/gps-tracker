# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Abstraction of the signaling pipeline to ensure deterministic testability of network lifecycles.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

*(No critical open gaps identified in the current hardening baseline)*

---

## 🟢 Recently Evaluated Issues & Status

1. **Issue #1050/1052: High-Fidelity Doze Integration & Telemetry Gating Gap**
    *   *Status*: Resolved. Patched `ConnectivitySuite` (sync loops and telemetry emission) to respect `UnifiedPowerPolicy.shouldDeferSignaling()`. Implemented `PowerIntegrationAuditTest.kt` to verify the `PowerManager` bridge via `adb shell` Doze simulation. Ensured violation-triggered bypass for real-time responsiveness (Resolved Sep.16.13 / R-ID 351).

2. **Issue #1072: Static State Leakage in Test Fakes**
    *   *Status*: Resolved. Implemented a reset mechanism in `ProductionReadinessAuditTest.kt`'s `@Before` block to ensure `FakePowerStateProvider.isIdle` is cleared before every test case (Resolved Sep.16.12 / R-ID 350).

3. **Issue #1060: UI Sampling Refinement**
    *   *Status*: Confirmed that `MainViewModel.kt` has been fully transitioned to use direct `PerformanceTier` enum comparisons for all `.sample()` intervals.

4. **Issue #20: AndroidNetworkProvider Race Condition**
    *   *Status*: Resolved race condition in asynchronous unregistration by serializing all platform state transitions on the Main Looper (Resolved Sep.16.11 / R-ID 349).

5. **Issue #20: Signaling Pipeline Hardening**
    *   *Status*: Enforced HTTP 2xx check for keep-alive probes in `ConnectivitySuite` (R-ID 349).

6. **Issue #20: Signaling Pipeline Abstraction**
    *   *Status*: Decoupled `ConnectivitySuite` from Android's `ConnectivityManager` and direct HTTP calls (R-ID 349).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 351 (Rules: 70, IDs: 351), Resolved: 1078, Open: 0, Testing: 0 (Sub-items: 0), Ideas: 18, QA: 280]**
