# Project Issues & Hardening Tracking (Rigorous Audit) - Oct18.8

## 🎯 Current Resumption Focus: Doze Mode White-listing (Chapter 37).

## 🔴 Open Gaps & Unfinished Integration Points
*   *No high or medium priority gaps remaining for the current baseline.*

---

## 💡 Strategic Simplification Ideas (Ideas: 45)
*   **SIMP-OCT18.14-S1: Altitude-Baro Fusion for Anchors (Medium).** Use barometric trends to further validate GNSS altitude shifts during high-SNR reflections to prevent false breakouts.
*   **SIMP-OCT18.12-S1: Acoustic State Machine (Medium).** Refactor the nested duty-cycle logic in `HardwareSuite.kt` into a formal state machine to improve readability and testability of transition overlaps.
*   **SIMP-OCT18.10-S1: PID-Controlled Vibration Floor (Medium).** Replace multi-stage EMA with a simple PID controller to allow smoother but faster convergence to the vibration noise floor.
*   **SIMP-OCT18.8-S1: SNR-Aware Kalman Gain (Medium).** Dynamically scale process noise (Q) based on SNR stability to further dampen multipath without needing hard latches for all events.
*   **SIMP-OCT18.4-S1: Unified Trace Header (Low).** Consolidate `LogEntry` and `ForensicSnapshot` into a single flat structure for JNI transfers to further reduce allocation overhead during 100Hz sampling.
*   **SIMP-OCT18.0-S1: JNI Buffer Validator (Low).** Add a startup-time JNI function that verifies the size and alignment of `sharedStateBuffer` structures between Kotlin and C++.
*   **SIMP-OCT17.11-S2: Unified JNI Offset Mapping (Medium).** Centralize shared memory offsets in a single cross-layer definition file.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT18.14-F1: Urban Signal Reflectivity Hardening.** Resolved Oct18.8. Implemented `ANCHOR_REFLECTIVITY_ALTITUDE_GATE` (3.5m) skepticism in `AnchorEvaluator` to prevent multipath breakouts during high-SNR reflections with significant altitude shifts. (SOT ID 768).
*   **ISSUE-OCT18.12-F1: Acoustic Duty Cycle Transition Latency.** Resolved Oct18.8. Implemented `ACOUSTIC_DUTY_CYCLE_OVERLAP_MS` (500ms) buffering in `HardwareSuite` to ensure microphone warm-up and trailing audio capture during duty cycle transitions. (SOT ID 767).
*   **ISSUE-OCT18.10-F1: Adaptive Vibration Floor Convergence.** Resolved Oct18.8. Implemented Steep-Drop convergence logic (R-ID 736) to accelerate floor reset during high-to-low vibration transitions. Synchronized JNI and JVM paths. (SOT ID 766).
*   **ISSUE-OCT18.8-F1: Urban Multi-path Hardening.** Resolved Oct18.8. Implemented 360s SNR Latch (R-ID 735) and enhanced stationary anchor skepticism for high-SNR reflections. (SOT ID 765).
*   **ISSUE-OCT18.6-F1: Synthetic Physical Event Injection.** Resolved Oct18.8. Implemented simulation framework for remote validation. (SOT ID 764).

---

## 📊 Hardening Progress Dashboard
- **Oct18.8: [SOT Count: 460 (Rules: 294), Open: H:0, M:0, L:0, Ideas: H:1, M:19, L:25, Testing: 28 (Sub-items: 105), QA: 388]**
- **Oct18.8: [SOT Count: 459 (Rules: 293), Open: H:0, M:0, L:0, Ideas: H:1, M:18, L:25, Testing: 28 (Sub-items: 105), QA: 387]**
