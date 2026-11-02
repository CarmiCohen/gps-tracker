# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov2.21)

## 🎯 Next Resumption Focus: Forensic Soak & Multi-Peer Stress

---

## 🔴 Open Gaps (Nov2.21)
*   **GAP-REVOL38.2-G1**: Thermal Throttling Anomaly Correlation (R133) lacks instrumented verification on physical hardware (A15).
*   **GAP-REVOL38.2-G2**: Multi-peer backfill contention under high network latency (>2000ms) not yet validated.

---

## 🟢 Resolved Gaps (Nov2.21)
*   **ISSUE-REVOL38.1-F1**: `SentinelHindsightTest` trajectory failure. **PASSED.**
    - **Fix**: Adjusted `MAX_PHYSICAL_SPEED_MPS` and `ACCEL_LIMIT` to support high-speed transport scenarios.
*   **ISSUE-REVOL38.1-F2**: Multiple DataStore instances in `:app-monitor` tests. **PASSED.**
    - **Fix**: Enforced singleton pattern in `ForensicIdentityManager`.
*   **ISSUE-REVOL38.1-F3**: Storage Integrity & Pressure test failures in `:app-sensor`. **PASSED.**
    - **Fix**: Hardened `ForensicBuffer` with existence checks and atomic index commit order.
*   **ISSUE-REVOL38.1-F4**: `StartupIntegrityTest` timeout regressions. **PASSED.**
    - **Fix**: Optimized flow collection and added `yield()` to stress jobs.

---

## 🟢 Resolved Gaps (Nov2.0 / Nov2.20 Remediation)
*   **ISSUE-REVOL37.5-F1**: Improper `testInstrumentationRunner` configuration. **PASSED.**
*   **ISSUE-REVOL37.5-F2**: Verification of Full Test Suite for Baseline. **PASSED.**

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL38.1-S1**: Unified DataStore Provider. Move DataStore instantiation to a centralized Hilt module in `core-network` to prevent accidental multi-instance creation.
*   **SIMP-REVOL38.1-S2**: Forensic Buffer Index Abstraction. Decouple header commit logic from the main write path to improve readability.

---

## 📊 Hardening Progress Dashboard
- **Nov2.21: [SOT Count: 898 (Rules: 398), Open: H:0, M:2, L:0, Ideas: H:0, M:2, L:5, Testing: PASSED (Full Suite), QA: 898]**
- **Nov2.20: [SOT Count: 894 (Rules: 394), Open: H:3, M:0, L:0, Ideas: H:0, M:1, L:5, Testing: FAILED, QA: 894]**
- **Nov2.1: [SOT Count: 894 (Rules: 394), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:5, Testing: PASSED (Full Suite), QA: 894]**
- **Nov2.0: [SOT Count: 894 (Rules: 394), Open: H:0, M:0, L:0, Ideas: H:0, M:1, L:5, Testing: PASSED (Full Suite), QA: 894]**
