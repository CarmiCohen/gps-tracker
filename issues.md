# Project Issues & Hardening Tracking (Rigorous Audit) - Oct17.11

## 🎯 Current Resumption Focus: COMPLETED Full System Audit.

## 🔴 Open Gaps & Unfinished Integration Points
*   *None. All tests PASSED.*

---

## 💡 Strategic Simplification Ideas (Ideas: 39)
*   **SIMP-OCT17.11-S1: Test Mode Dependency Injection (High).** Refactor `LogRepository` to use a dedicated `TestModeProvider` interface instead of a static `AtomicBoolean`. This would allow Hilt to inject a mock provider during instrumentation tests, eliminating the need for manual `setTestMode(true)` calls in `@Before` blocks.
*   **SIMP-OCT17.10-S1: Test Result Automation (Medium).** Automate the update of `test_results.md` from logcat or instrumentation output to reduce manual error during test cycles.
*   **SIMP-OCT17.4-S1: Ltr Force for Telemetry Controls (Low).** Force Ltr direction for all telemetry sliders and hex-color displays to prevent forensic misinterpretation in RTL locales.

---

## ✅ Resolved Traceability & Metadata Issues
*   **TEST-OCT17.11-T1: Automated Test Suite Synchronization.** Resolved Oct17.11. Synchronized unit tests (`MainViewModelMappingTest`) and instrumentation tests (`ForensicStressAuditTest`) with version `Oct17.11`. Fixed Compose BOM alignment in `app/build.gradle`. (SOT ID 759, 758).
*   **TEST-OCT17.10-1: Manual Test Cycle Resumption & Version Hardening.** Resolved Oct14.1. Verified permission onboarding (Ch 1.4) and synchronized version branding to Oct14.1_PLATINUM. (SOT ID 757, 756).
*   **SIMP-OCT15.6-S1: Automated Version Synchronization.** Resolved Oct14.1. Centralized `versionName` in `gradle.properties`. (SOT ID 757).

---

## 📊 Hardening Progress Dashboard
- **Oct17.11: [SOT Count: 451 (Rules: 286), Open: H:0, M:0, L:0, Ideas: H:2, M:14, L:23, Testing: 27 (Sub-items: 167), QA: 372]**
- **Oct14.1: [SOT Count: 449 (Rules: 286), Open: H:0, M:0, L:0, Ideas: H:1, M:14, L:22, Testing: 23 (Sub-items: 163), QA: 368]**
