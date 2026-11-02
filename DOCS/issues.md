# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov1.7)

## 🎯 Next Resumption Focus: Final Forensic Verification

---

## 🟢 Remediated Gaps (Nov1.7) - PENDING VERIFICATION
*   **ISSUE-REVOL37.1-F1**: Foreground Service Permission Regression. **REMEDIATED.**
    - **Fix**: Deferred sensitive component initialization in `SensorService` to `onStartCommand` after `hasRequiredPermissions()` check. Added runtime check for `FOREGROUND_SERVICE_LOCATION` and `ACCESS_NETWORK_STATE`.
*   **ISSUE-REVOL37.1-F2**: Forensic Audit Signature Leak. **REMEDIATED.**
    - **Fix**: Silenced `StackLog` signatures by preventing `SecurityException` leaks through pre-emptive permission checks and guarded initialization blocks.

---

## 🔴 Open Gaps (Nov1.7)
*   *None. Awaiting hardware verification.*

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL30.0-S1**: Shared Test Fixtures. Consolidate hardware mocks into a test-only artifact within `:core-shared`.
*   **SIMP-REVOL33.0-S1**: Batch Buffer Pooling. Implement object pooling for telemetry batch JSON arrays.
*   **SIMP-REVOL37.0-S1**: Hilt Test Module Consolidation. Host common Hilt overrides in a `core-test` module.

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.6)
*   **ISSUE-REVOL36.1-F1**: Hilt Test Infrastructure Hardening. **RESOLVED.** Fixed duplicate Dagger bindings in `:app-monitor`. (R-ID 84).

---

## 📊 Hardening Progress Dashboard
- **Nov1.7: [SOT Count: 891 (Rules: 391), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:5, Testing: REMEDIATED (Pending Verification), QA: 891]**
- **Nov1.6: [SOT Count: 891 (Rules: 391), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:5, Testing: PASSED (Infrastructure Fix), QA: 891]**
- **Nov1.5: [SOT Count: 891 (Rules: 391), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: PASSED (Watchdog & Backfill), QA: 891]**
