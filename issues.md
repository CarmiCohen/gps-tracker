# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.1

## 🎯 Current Resumption Focus: Urban Multi-path Hardening (Chapter 6).

## 🔴 Open Gaps & Unfinished Integration Points
*   *No high or medium priority gaps remaining for the current baseline.*

---

## 💡 Strategic Simplification Ideas (Ideas: 41)
*   **SIMP-OCT18.4-S1: Unified Trace Header (Low).** Consolidate `LogEntry` and `ForensicSnapshot` into a single flat structure for JNI transfers to further reduce allocation overhead during 100Hz sampling.
*   **SIMP-OCT18.0-S1: JNI Buffer Validator (Low).** Add a startup-time JNI function that verifies the size and alignment of `sharedStateBuffer` structures between Kotlin and C++.
*   **SIMP-OCT17.11-S2: Unified JNI Offset Mapping (Medium).** Centralize shared memory offsets in a single cross-layer definition file.
*   **SIMP-OCT17.10-S1: Test Result Automation (Medium).** Automate the update of `test_results.md` from logcat or instrumentation output.
*   **SIMP-OCT17.4-S1: Ltr Force for Telemetry Controls (Low).** Force Ltr direction for all telemetry sliders and hex-color displays.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT18.6-F1: Synthetic Physical Event Injection.** Resolved Oct14.1. Implemented a centralized simulation framework to bridge the "Physical Gap" for remote validation of Chapters 3.2, 5.1, 15.1, and 16.1. Added hooks to `HardwareSuite` and `NetworkProvider`. (SOT ID 764).
*   **SIMP-OCT18.2-S1: Explicit Repository Lifecycle.** Resolved Oct14.1. Moved background job initiation from `LogRepository.init` to an explicit `start()` method called by `GpsApplication`. Removed auto-starting logic from constructors to ensure predictable initialization order and better test isolation. (SOT ID 763).
*   **SIMP-OCT17.11-S1: Test Mode Dependency Injection.** Resolved Oct14.1. Refactored `LogRepository` to use a dedicated `TestModeProvider` interface instead of a static `AtomicBoolean`. (SOT ID 762).
*   **ISSUE-OCT16.9-F2: Vibration NaNG Hardening.** Resolved Oct14.1. Fixed JNI buffer misalignment in `GnssHealthBatch` by re-aligning to 8-byte boundaries. (SOT ID 761).
*   **TEST-OCT17.11-T2: vOct14.1 Full System Audit.** Resolved Oct14.1. Completed the logic and UI navigation phase of the PLATINUM release. (SOT ID 760).
*   **TEST-OCT17.11-T1: Automated Test Suite Synchronization.** Resolved Oct14.1. Synchronized unit and instrumentation tests with version `Oct14.1`. (SOT ID 759, 758).
*   **TEST-OCT17.10-1: Manual Test Cycle Resumption & Version Hardening.** Resolved Oct14.1. Verified permission onboarding and synchronized version branding. (SOT ID 757, 756).

---

## 📊 Hardening Progress Dashboard
- **Oct14.1: [SOT Count: 456 (Rules: 290), Open: H:0, M:0, L:0, Ideas: H:1, M:15, L:25, Testing: 28 (Sub-items: 105), QA: 384]**
- **Oct14.1: [SOT Count: 455 (Rules: 289), Open: H:0, M:1, L:0, Ideas: H:1, M:15, L:25, Testing: 27 (Sub-items: 101), QA: 383]**
- **Oct14.1: [SOT Count: 454 (Rules: 288), Open: H:0, M:1, L:0, Ideas: H:1, M:16, L:25, Testing: 27 (Sub-items: 101), QA: 382]**
- **Oct14.1: [SOT Count: 453 (Rules: 287), Open: H:0, M:1, L:0, Ideas: H:2, M:15, L:25, Testing: 27 (Sub-items: 101), QA: 381]**
