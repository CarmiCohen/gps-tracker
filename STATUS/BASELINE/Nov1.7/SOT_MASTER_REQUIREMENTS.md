# SOT Master Requirements & Hardening Status (Nov1.7)

## 🏗️ Architectural Master Rules (393 Rules)

### 1. Lifecycle & Resource Management
*   ... (Rules 1.1 to 1.316 preserved)
*   **1.317 Forensic Silence & API 34 Permission Hardening (R-ID 86)**: The `SensorService` MUST defer initialization of all sensitive hardware and network components until after runtime permissions are explicitly verified. (Nov1.7 - ISSUE-REVOL37.1-F1, ISSUE-REVOL37.1-F2).
*   **1.318 Hilt Test Infrastructure Stabilization (R-ID 87)**: All instrumented tests utilizing `HiltAndroidRule` MUST declare the rule as a `val` to ensure deterministic component creation and injection timing, preventing `IllegalStateException` during high-frequency hardening audits. (Nov1.7 - ISSUE-REVOL37.2-F1).

## 🛡️ Core Hardening Baseline
- **Nov1.7**: Forensic Audit & Hilt Stabilization - PASSED. Resolved API 34 regressions, forensic leaks, and test injection failures.
- **Nov1.6**: Hilt Test Infrastructure Hardening - PASSED.
... (remaining baseline preserved)

## ✅ Verification Chapters
*   **Chapter 5.19 (Nov1.7 Operational Forensic Audit)**: PASSED - Verified zero Davey events > 500ms, zero StackLog leaks, and Hilt injection stability.
... (remaining chapters preserved)
