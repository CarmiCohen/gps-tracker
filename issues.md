# Project Issues & Hardening Tracking (Rigorous Audit) - Oct17.10

## 🎯 Current Resumption Focus: Manual Test Procedure - Chapter 2.1 (Tracker Mode).

## 🔴 Open Gaps & Unfinished Integration Points
*   *None. All initial regressions resolved.*

---

## 💡 Strategic Simplification Ideas (Ideas: 38)
*   **SIMP-OCT17.10-S1: Test Result Automation (Medium).** Automate the update of `test_results.md` from logcat or instrumentation output to reduce manual error during test cycles.
*   **SIMP-OCT17.4-S1: Ltr Force for Telemetry Controls (Low).** Force Ltr direction for all telemetry sliders and hex-color displays to prevent forensic misinterpretation in RTL locales.
*   **SIMP-OCT17.1-S1: Centralized Versioning Authority (Low).** Consider moving the versionName to a central constants.gradle or libs.versions.toml to avoid hardcoded UI strings.

---

## ✅ Resolved Traceability & Metadata Issues
*   **TEST-OCT17.10-1: Manual Test Cycle Resumption & Version Hardening.** Resolved Oct17.10. Verified permission onboarding (Ch 1.4) and synchronized version branding to Oct17.10_PLATINUM. (SOT ID 757, 756).
*   **SIMP-OCT15.6-S1: Automated Version Synchronization.** Resolved Oct14.1. Centralized `versionName` in `gradle.properties` as `APP_VERSION_NAME`. Updated `app/build.gradle` to inject this into `BuildConfig`. Refactored `DashboardMapper.kt` and `MainAppContent.kt` to use `BuildConfig.VERSION_NAME` instead of hardcoded strings. (SOT ID 757).
*   **ISSUE-OCT15.6-T1: Version Name Test Procedure.** Added formal version verification to `TEST_PROCEDURE.md` (Step 1.3). Synchronized `app/build.gradle`, `MainAppContent.kt`, and `DashboardMapper.kt` to version `Oct14.1`. Verified in `test_results.md`.
*   **ISSUE-OCT17.4-F2: RTL Slider Inversion on A15.** Resolved Oct14.1. Wrapped `SensitivitySlider` in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` in `SettingsComponents.kt` (Line 285). Verified on A15 hardware.
*   **ISSUE-OCT17.4-F1: Regression: Vibration Telemetry NaN in HUD.** Resolved Oct14.1. Replaced direct pointer casting with alignment-safe `memcpy` in `jdhardware-jni.cpp` for all 8-byte primitives. Verified numeric telemetry on Samsung A15. (SOT ID 734).
*   **DOC-OCT17.4: Version Synchronization.** Resolved Oct14.1. Updated hardcoded version label in `MainAppContent.kt` and synchronized `DashboardMapper.kt` engine string.
*   **DOC-OCT17.3: Full Project Test Cycle.** Executed full automated suite on A15 hardware. All tests PASSED.

---

## 📊 Hardening Progress Dashboard
- **Oct17.10: [SOT Count: 449 (Rules: 286), Open: H:0, M:0, L:0, Ideas: H:1, M:14, L:22, Testing: 23 (Sub-items: 163), QA: 368]**
- **Oct14.1: [SOT Count: 449 (Rules: 286), Open: H:0, M:0, L:0, Ideas: H:1, M:13, L:22, Testing: 23 (Sub-items: 163), QA: 368]**
- **Oct14.1: [SOT Count: 448 (Rules: 285), Open: H:0, M:0, L:0, Ideas: H:1, M:13, L:23, Testing: 23 (Sub-items: 163), QA: 368]**
