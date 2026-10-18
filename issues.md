# Project Issues & Hardening Tracking (Rigorous Audit) - Oct18.0

## 🎯 Current Resumption Focus: Physical Validation Gap (Chapters 3.2, 15.1, 16.1).

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT17.11-P1: Physical Environment Verification (Medium).** Chapters 3.2, 5.1, 15.1, and 16.1 of the manual protocol require physical hardware interaction (shaking, unplugging, shielding) currently blocked by remote location.

---

## 💡 Strategic Simplification Ideas (Ideas: 42)
*   **SIMP-OCT18.0-S1: JNI Buffer Validator (Low).** Add a startup-time JNI function that verifies the size and alignment of `sharedStateBuffer` structures between Kotlin and C++ to detect misalignment regressions early.
*   **SIMP-OCT17.11-S2: Unified JNI Offset Mapping (Medium).** Centralize shared memory offsets in a single cross-layer definition file to prevent manual synchronization errors between Kotlin and C++.
*   **SIMP-OCT17.11-S1: Test Mode Dependency Injection (High).** Refactor `LogRepository` to use a dedicated `TestModeProvider` interface instead of a static `AtomicBoolean`. This would allow Hilt to inject a mock provider during instrumentation tests, eliminating the need for manual `setTestMode(true)` calls in `@Before` blocks.
*   **SIMP-OCT17.10-S1: Test Result Automation (Medium).** Automate the update of `test_results.md` from logcat or instrumentation output to reduce manual error during test cycles.
*   **SIMP-OCT17.4-S1: Ltr Force for Telemetry Controls (Low).** Force Ltr direction for all telemetry sliders and hex-color displays to prevent forensic misinterpretation in RTL locales.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT16.9-F2: Vibration NaNG Hardening.** Resolved Oct18.0. Fixed JNI buffer misalignment in `GnssHealthBatch` by re-aligning to 8-byte boundaries. Reinforced `jdhardware-jni.cpp` with `isnan()` sanitization for `vibeNorm`, `snr`, `thermal`, and `heap` inputs. (SOT ID 761).
*   **TEST-OCT17.11-T2: vOct14.1 Full System Audit.** Resolved Oct14.1. Completed the logic and UI navigation phase of the PLATINUM release. Verified 100 manual chapters (logical) and 27 instrumentation tests. (SOT ID 760).
*   **TEST-OCT17.11-T1: Automated Test Suite Synchronization.** Resolved Oct14.1. Synchronized unit tests (`MainViewModelMappingTest`) and instrumentation tests (`ForensicStressAuditTest`) with version `Oct14.1`. Fixed Compose BOM alignment in `app/build.gradle`. (SOT ID 759, 758).
*   **TEST-OCT17.10-1: Manual Test Cycle Resumption & Version Hardening.** Resolved Oct14.1. Verified permission onboarding (Ch 1.4) and synchronized version branding to Oct14.1_PLATINUM. (SOT ID 757, 756).
*   **SIMP-OCT15.6-S1: Automated Version Synchronization.** Resolved Oct14.1. Centralized `versionName` in `gradle.properties`. (SOT ID 757).

---

## 📊 Hardening Progress Dashboard
- **Oct18.0: [SOT Count: 453 (Rules: 287), Open: H:0, M:1, L:0, Ideas: H:2, M:15, L:25, Testing: 27 (Sub-items: 101), QA: 381]**
- **Oct14.1: [SOT Count: 452 (Rules: 286), Open: H:1, M:1, L:0, Ideas: H:2, M:16, L:23, Testing: 27 (Sub-items: 100), QA: 380]**
