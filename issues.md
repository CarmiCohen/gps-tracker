# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.1

## 🎯 Current Resumption Focus: Physical Validation Gap (Chapters 3.2, 15.1, 16.1).

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT17.11-F1: JNI Buffer NaNG Regression (High).** "Vibration" HUD fields display `NaNG` during extreme I/O load on A15 hardware. Requires strict 8-byte boundary alignment and `isnan()` reinforcement in `jdhardware-jni.cpp`.
*   **ISSUE-OCT17.11-P1: Physical Environment Verification (Medium).** Chapters 3.2, 5.1, 15.1, and 16.1 of the manual protocol require physical hardware interaction (shaking, unplugging, shielding) currently blocked by remote location.

---

## 💡 Strategic Simplification Ideas (Ideas: 41)
*   **SIMP-OCT17.11-S2: Unified JNI Offset Mapping (Medium).** Centralize shared memory offsets in a single cross-layer definition file to prevent manual synchronization errors between Kotlin and C++.
*   **SIMP-OCT17.11-S1: Test Mode Dependency Injection (High).** Refactor `LogRepository` to use a dedicated `TestModeProvider` interface instead of a static `AtomicBoolean`. This would allow Hilt to inject a mock provider during instrumentation tests, eliminating the need for manual `setTestMode(true)` calls in `@Before` blocks.
*   **SIMP-OCT17.10-S1: Test Result Automation (Medium).** Automate the update of `test_results.md` from logcat or instrumentation output to reduce manual error during test cycles.
*   **SIMP-OCT17.4-S1: Ltr Force for Telemetry Controls (Low).** Force Ltr direction for all telemetry sliders and hex-color displays to prevent forensic misinterpretation in RTL locales.

---

## ✅ Resolved Traceability & Metadata Issues
*   **TEST-OCT17.11-T2: vOct14.1 Full System Audit.** Resolved Oct14.1. Completed the logic and UI navigation phase of the PLATINUM release. Verified 100 manual chapters (logical) and 27 instrumentation tests. (SOT ID 760).
*   **TEST-OCT17.11-T1: Automated Test Suite Synchronization.** Resolved Oct14.1. Synchronized unit tests (`MainViewModelMappingTest`) and instrumentation tests (`ForensicStressAuditTest`) with version `Oct14.1`. Fixed Compose BOM alignment in `app/build.gradle`. (SOT ID 759, 758).
*   **TEST-OCT17.10-1: Manual Test Cycle Resumption & Version Hardening.** Resolved Oct14.1. Verified permission onboarding (Ch 1.4) and synchronized version branding to Oct14.1_PLATINUM. (SOT ID 757, 756).
*   **SIMP-OCT15.6-S1: Automated Version Synchronization.** Resolved Oct14.1. Centralized `versionName` in `gradle.properties`. (SOT ID 757).

---

## 📊 Hardening Progress Dashboard
- **Oct14.1: [SOT Count: 452 (Rules: 286), Open: H:1, M:1, L:0, Ideas: H:2, M:16, L:23, Testing: 27 (Sub-items: 100), QA: 380]**
- **Oct14.1: [SOT Count: 451 (Rules: 286), Open: H:0, M:0, L:0, Ideas: H:2, M:14, L:23, Testing: 27 (Sub-items: 167), QA: 372]**
