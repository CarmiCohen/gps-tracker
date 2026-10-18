# Project Issues & Hardening Tracking (Rigorous Audit) - Oct18.8

## 🎯 Current Resumption Focus: Adaptive Vibration Floor Convergence (Chapter 34).

## 🔴 Open Gaps & Unfinished Integration Points
*   *No high or medium priority gaps remaining for the current baseline.*

---

## 💡 Strategic Simplification Ideas (Ideas: 42)
*   **SIMP-OCT18.8-S1: SNR-Aware Kalman Gain (Medium).** Dynamically scale process noise (Q) based on SNR stability to further dampen multipath without needing hard latches for all events.
*   **SIMP-OCT18.4-S1: Unified Trace Header (Low).** Consolidate `LogEntry` and `ForensicSnapshot` into a single flat structure for JNI transfers to further reduce allocation overhead during 100Hz sampling.
*   **SIMP-OCT18.0-S1: JNI Buffer Validator (Low).** Add a startup-time JNI function that verifies the size and alignment of `sharedStateBuffer` structures between Kotlin and C++.
*   **SIMP-OCT17.11-S2: Unified JNI Offset Mapping (Medium).** Centralize shared memory offsets in a single cross-layer definition file.
*   **SIMP-OCT17.10-S1: Test Result Automation (Medium).** Automate the update of `test_results.md` from logcat or instrumentation output.
*   **SIMP-OCT17.4-S1: Ltr Force for Telemetry Controls (Low).** Force Ltr direction for all telemetry sliders and hex-color displays.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT18.8-F1: Urban Multi-path Hardening.** Resolved Oct18.8. Implemented 360s SNR Latch (R-ID 735) and enhanced stationary anchor skepticism for high-SNR reflections. Propagated `isHighSnrReflection` flag through engine layers to ensure geofence stability in urban canyons. (SOT ID 765).
*   **ISSUE-OCT18.6-F1: Synthetic Physical Event Injection.** Resolved Oct18.8. Implemented a centralized simulation framework to bridge the "Physical Gap" for remote validation of Chapters 3.2, 5.1, 15.1, and 16.1. Added hooks to `HardwareSuite` and `NetworkProvider`. (SOT ID 764).
*   **SIMP-OCT18.2-S1: Explicit Repository Lifecycle.** Resolved Oct18.8. Moved background job initiation from constructors to explicit `start()` triggers. (SOT ID 763).
*   **SIMP-OCT17.11-S1: Test Mode Dependency Injection.** Resolved Oct18.8. Refactored `LogRepository` to use `TestModeProvider`. (SOT ID 762).
*   **ISSUE-OCT16.9-F2: Vibration NaNG Hardening.** Resolved Oct18.8. Fixed JNI buffer misalignment in `GnssHealthBatch`. (SOT ID 761).

---

## 📊 Hardening Progress Dashboard
- **Oct18.8: [SOT Count: 457 (Rules: 291), Open: H:0, M:0, L:0, Ideas: H:1, M:16, L:25, Testing: 28 (Sub-items: 105), QA: 385]**
- **Oct18.8: [SOT Count: 456 (Rules: 290), Open: H:0, M:0, L:0, Ideas: H:1, M:15, L:25, Testing: 28 (Sub-items: 105), QA: 384]**
