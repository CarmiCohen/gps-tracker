# Project Issues & Hardening Tracking (Rigorous Audit) - Oct20.7

## 🎯 Current Resumption Focus: Storage Test Race Conditions (#ISSUE-OCT18.22-F3).

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT18.22-F3: Storage Test Race Conditions (Low).** Intermittent failures in `StoragePressureAuditTest` suggest non-determinism in manual log flushing.
    *   *Status*: Work in progress. Implemented `flushLogs()` and `flushForensicTraces()` in `LogRepository.kt` to allow synchronous persistence during tests. Integrated into `StoragePressureAuditTest.kt`. Needs final hardware verification.

---

## 💡 Strategic Simplification Ideas (Ideas: 52)
*   **SIMP-OCT20.7-S1: Automated Documentation Sharding (Low).** Consider implementing a build-time check or script that automatically shards `.md` files into archives when they exceed a specific line count (e.g., 500 lines) to prevent manual Rule 8 violations and IDE lag.
*   **SIMP-OCT20.2-S1: Deterministic Audit Verification (Low).** Consider integrating `logRepository.flushLogs()` into a base test rule to automatically ensure deterministic persistence across all forensic instrumentation tests.
*   **SIMP-OCT19.20-S1: Automated Doc-Sync Hook (Low).** Consider implementing a pre-commit hook that verifies `SOT_MASTER_REQUIREMENTS.md` length and header count against `RESOLUTION_ARCHIVE.md` to prevent future accidental truncations.
*   **SIMP-OCT19.19-S1: Unified Restoration Policy (Low).** Consider moving the restoration bypass logic from `MainViewModel` into `SessionUseCase` to centralize all session startup decisions and reduce ViewModel complexity.
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
*   **ISSUE-OCT20.7-F1: Forensic Document Archival.** Resolved Oct20.7. Optimized documentation by migrating legacy SOT IDs and resolutions to `docs_history_archive.md`. (SOT ID 780).
*   **ISSUE-OCT20.3-F1: Hardware Profile Test Regression.** Resolved Oct20.3. Fixed compilation failure in `HardwareSuiteProfileTest.kt` by updating mocks for R-ID 742 compliance. (SOT ID 779).
*   **ISSUE-OCT19.0-F1: Viewer with Simulated Tracker.** Resolved Oct20.2. Hardened mode restoration bypass in `MainViewModel` and validated peer simulation. (SOT ID 778).
*   **ISSUE-OCT20.0-F2: Testing Guideline Hardening.** Resolved Oct20.0. Updated protocol to enforce forensic preservation and Rule 8 doc-checks. (SOT ID 777).
*   **ISSUE-OCT20.0-F1: Document Integrity Baseline.** Resolved Oct20.0. Established a versioned baseline directory (`STATUS/BASELINE/Oct18.8/`) for critical status documents. (SOT ID 775).

---

## 📊 Hardening Progress Dashboard
- **Oct20.7: [SOT Count: 780 (Rules: 301), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:32, Testing: 100 (Sub-items: 101), QA: 351]**
- **Oct20.3: [SOT Count: 779 (Rules: 301), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:31, Testing: 100 (Sub-items: 101), QA: 350]**
- **Oct20.2: [SOT Count: 778 (Rules: 301), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:31, Testing: 100 (Sub-items: 101), QA: 349]**
- **Oct20.0: [SOT Count: 777 (Rules: 299), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:30, Testing: 100 (Sub-items: 101), QA: 348]**
