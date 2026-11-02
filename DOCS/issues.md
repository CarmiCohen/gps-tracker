# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov1.7)

## 🎯 Next Resumption Focus: UI Hardening & R-ID 615 Verification

---

## 🟢 Resolved Gaps (Nov1.7)
*   **ISSUE-REVOL37.1-F1**: Foreground Service Permission Regression. **PASSED.**
*   **ISSUE-REVOL37.1-F2**: Forensic Audit Signature Leak. **PASSED.**
*   **ISSUE-REVOL37.2-F1**: Hilt Test Injection Failure in `:app-sensor`. **PASSED.**

---

## 🔴 Open Gaps (Nov1.7)
*   **ISSUE-REVOL37.3-F1**: Missing Settings Gear on Tracker Dashboard. (R797)
*   **ISSUE-REVOL37.3-F2**: Missing "Load Factory Defaults" and Configuration UI in Admin Console.
    - **Symptom**: `AdminActivity` lacks mandatory buttons for factory reset and ID configuration required by `TEST_PROCEDURE.md` Chapter 2.5.
    - **Severity**: High (Protocol Blocker).
    - **Remediation Status**: Fixing in `AdminActivity.kt`.

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL37.0-S1**: Hilt Test Module Consolidation. Host common Hilt overrides in a `core-test` module.

---

## 📊 Hardening Progress Dashboard
- **Nov1.7: [SOT Count: 893 (Rules: 393), Open: H:2, M:0, L:0, Ideas: H:0, M:0, L:5, Testing: FAILED (2.5), QA: 893]**
