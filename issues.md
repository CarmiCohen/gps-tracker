# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.1

## 🎯 Current Resumption Focus: Post-Test Cycle Monitoring & Production Readiness

## 🔴 Open Gaps & Unfinished Integration Points
*   (None) - Full Project Test Cycle completed in Oct14.1.

---

## 💡 Strategic Simplification Ideas (Ideas: 36)
*   **SIMP-OCT17.1-S1: Centralized Versioning Authority (Low).** Consider moving the `versionName` to a central `constants.gradle` or `libs.versions.toml` that all `.md` status files can reference via a script to prevent manual synchronization overhead.
*   **SIMP-OCT16.11-S1: JNI FastPath Vectorization (Medium).** Explore NEON/SIMD for vibration batch processing to further reduce CPU load on ARMv8 devices.
*   **SIMP-OCT16.10-S1: JNI Buffer Schema Generator (High).** Create a simple script to generate both Kotlin `ByteBuffer` offsets and C++ `struct` offsets from a single YAML definition to prevent future alignment regressions.
*   **SIMP-OCT16.2-S1: Repository State Projection (Low).** Consider extracting a `StateProvider` interface from `MainRepository` to allow UseCases to observe specific sub-states without full repository access.
*   **SIMP-OCT16.9-S1: Unified Violation Evaluation (Medium).** Consolidate `isTracker` guards into a `ViolationEvaluator` that understands if the source data is local or remote, instead of hardcoding role checks in `MainAlarmLogic`.
*   **SIMP-OCT16.9-S2: Vibration EMA Boundary Check (Low).** Add `isNaN` guards to `SentinelForensicState` updates to prevent telemetry poisoning.

---

## ✅ Resolved Traceability & Metadata Issues
*   **DOC-OCT17.3: Full Project Test Cycle.** Executed full automated suite (43 unit, 27 instrumented) on A15 hardware. All tests PASSED. Incremented version to Oct14.1. (SOT ID 755).
*   **DOC-OCT17.2: Version Synchronization.** Aligned all status documentation with `app/build.gradle` (Oct14.1).
*   **ISSUE-OCT16.11-F1: JNI Telemetry Integration Hardening.** Migrated Acoustic spike detection and advanced Vibration gates (Jamming, Muzzle, Pressure) from Kotlin placeholders to full C++ implementations. Verified physics-parity in `EngineConstants.h`. (R-ID 603, SOT ID 753).
*   **ISSUE-OCT16.9-F2: Regression: Vibration Telemetry NaN in HUD.** Resolved HUD `NaNG` by hardening JNI `n19` with `isnan()` guards and enforcing strict 8-byte alignment for 64-bit primitives in the shared buffer. (R-ID 602).
*   **ISSUE-OCT16.9-F1: Regression: Viewer Remote Alarm Authority Broken.** Fixed regression where `isTracker` guards prevented Viewers from siren-alerting for remote physical violations. Verified via `AlarmStealthTest`.
*   **ISSUE-OCT16.8-F1: Missing Termination Confirmation Dialog.** Implemented `DialogHost` in `MainAppContent.kt` to ensure termination dialogs are not obscured by overlays. (SIMP-OCT16.8-S1)
*   **ISSUE-OCT16.8-F2: Role Transition Grace Period Violation.** Added 30s hardware boot grace to `AppAlarmManager` and fixed `isTrackerMode` atomic synchronization.

---

## 📊 Hardening Progress Dashboard
- **Oct14.1: [SOT Count: 447 (Rules: 284), Open: H:0, M:0, L:0, Ideas: H:1, M:13, L:22, Testing: 23 (Sub-items: 162), QA: 367]**
- **Oct14.1: [SOT Count: 446 (Rules: 284), Open: H:0, M:0, L:0, Ideas: H:1, M:13, L:22, Testing: 23 (Sub-items: 162), QA: 366]**
- **Oct14.1: [SOT Count: 446 (Rules: 284), Open: H:0, M:0, L:0, Ideas: H:1, M:13, L:22, Testing: 23 (Sub-items: 162), QA: 365]**
- **Oct14.1: [SOT Count: 446 (Rules: 284), Open: H:0, M:0, L:0, Ideas: H:1, M:13, L:21, Testing: 23 (Sub-items: 162), QA: 364]**

---

## 🛠️ Strategic Simplification Ideas (Audit Record)
*   **SIMP-OCT17.1-S1**: Significance: Low. Centralized Versioning.
*   **SIMP-OCT16.11-S1**: Significance: Medium. NEON Vectorization.
*   **SIMP-OCT16.10-S1**: Significance: High. JNI Buffer Generator.
