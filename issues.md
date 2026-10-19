# Project Issues & Hardening Tracking (Rigorous Audit) - Oct18.8

## 🎯 Current Resumption Focus: Storage Test Race Conditions (#OCT18.22-F3).

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT18.22-F3: Storage Test Race Conditions (Low).** Intermittent failures in `StoragePressureAuditTest` suggest non-determinism in manual log flushing.

---

## 💡 Strategic Simplification Ideas (Ideas: 48)
*   **SIMP-OCT18.24-S1: Reactive Signaling Deferral (Low).** Consider unifying the shouldDeferSignaling check into a single Flow<Boolean> that ConnectivitySuite can collect, reducing repetitive polling and improving state synchronization.
*   **SIMP-OCT18.18-S1: Manual Processor Reification (Low).** Formalize the `triggerManualBatchProcessor` into a `TestingCommand` stream to avoid adding `@VisibleForTesting` methods to production repositories.
*   **SIMP-OCT18.16-S1: Unified Power Intent Handler (Low).** Consolidate `ACTION_POWER_SAVE_MODE_CHANGED` and `ACTION_DEVICE_IDLE_MODE_CHANGED` into a single reactive stream in `AndroidPowerStateProvider` to reduce receiver overhead.
*   **SIMP-OCT18.14-S1: Altitude-Baro Fusion for Anchors (Medium).** Use barometric trends to further validate GNSS altitude shifts during high-SNR reflections to prevent false breakouts.
*   **SIMP-OCT18.12-S1: Acoustic State Machine (Medium).** Refactor the nested duty-cycle logic in `HardwareSuite.kt` into a formal state machine to improve readability and testability of transition overlaps.
*   **SIMP-OCT18.10-S1: PID-Controlled Vibration Floor (Medium).** Replace multi-stage EMA with a simple PID controller to allow smoother but faster convergence to the vibration noise floor.
*   **SIMP-OCT18.8-S1: SNR-Aware Kalman Gain (Medium).** Dynamically scale process noise (Q) based on SNR stability to further dampen multipath without needing hard latches for all events.
*   **SIMP-OCT18.4-S1: Unified Trace Header (Low).** Consolidate `LogEntry` and `ForensicSnapshot` into a single flat structure for JNI transfers to further reduce allocation overhead during 100Hz sampling.
*   **SIMP-OCT18.0-S1: JNI Buffer Validator (Low).** Add a startup-time JNI function that verifies the size and alignment of `sharedStateBuffer` structures between Kotlin and C++.
*   **SIMP-OCT17.11-S2: Unified JNI Offset Mapping (Medium).** Centralize shared memory offsets in a single cross-layer definition file.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT18.22-F2: Redundant Network Callback Registrations.** Resolved Oct18.8. Consolidated all ConnectivityManager callback management into `AndroidNetworkProvider` and refactored `SystemStatusProviderImpl` to consume the unified flow. (SOT ID 772).
*   **ISSUE-OCT18.22-F1: Battery Saver Restricted Network behavior.** Resolved Oct18.8. Updated `ConnectivitySuite` logging to explicitly reflect `PowerSave` status in forensic traces, ensuring compliance with Chapter 38 audit signatures. (SOT ID 771).
*   **ISSUE-OCT18.18-F1: Storage Test Determinism.** Resolved Oct18.8. Added `triggerManualBatchProcessor()` to `LogRepository` to ensure deterministic log flushing during tests. (SOT ID 770).
*   **ISSUE-OCT18.16-F1: Doze Mode White-listing.** Resolved Oct18.8. Implemented `isIgnoringBatteryOptimizations` check in `PowerStateProvider` and updated `HardwareSuite` to allow signaling bypass during Doze mode for white-listed sessions. (SOT ID 769).
*   **ISSUE-OCT18.14-F1: Urban Signal Reflectivity Hardening.** Resolved Oct18.8. Implemented `ANCHOR_REFLECTIVITY_ALTITUDE_GATE` (3.5m) in `AnchorEvaluator` to prevent multipath breakouts during high-SNR reflections with significant altitude shifts. (SOT ID 768).
*   **ISSUE-OCT18.12-F1: Acoustic Duty Cycle Transition Latency.** Resolved Oct18.8. Implemented `ACOUSTIC_DUTY_CYCLE_OVERLAP_MS` (500ms) buffering in `HardwareSuite` to ensure microphone warm-up and trailing audio capture during duty cycle transitions. (SOT ID 767).
*   **ISSUE-OCT18.10-F1: Adaptive Vibration Floor Convergence.** Resolved Oct18.8. Implemented Steep-Drop convergence logic (R-ID 736) to accelerate floor reset during high-to-low vibration transitions. Synchronized JNI and JVM paths. (SOT ID 766).
*   **ISSUE-OCT18.8-F1: Urban Multi-path Hardening.** Resolved Oct18.8. Implemented 360s SNR Latch (R-ID 735) and enhanced stationary anchor skepticism for high-SNR reflections. (SOT ID 765).
*   **ISSUE-OCT18.6-F1: Synthetic Physical Event Injection.** Resolved Oct18.8. Implemented simulation framework for remote validation. (SOT ID 764).

---

## 📊 Hardening Progress Dashboard
- **Oct18.8: [SOT Count: 464 (Rules: 298), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:28, Testing: 100 (Sub-items: 100), QA: 392]**
- **Oct18.8: [SOT Count: 462 (Rules: 296), Open: H:0, M:1, L:2, Ideas: H:1, M:19, L:27, Testing: 100 (Sub-items: 100), QA: 390]**
- **Oct18.8: [SOT Count: 462 (Rules: 296), Open: H:0, M:0, L:0, Ideas: H:1, M:19, L:27, Testing: 100 (Sub-items: 100), QA: 390]**
