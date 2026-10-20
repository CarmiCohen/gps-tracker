# Project Issues & Hardening Tracking (Rigorous Audit) - Oct20.22

## 🎯 Current Resumption Focus: Final Audit & Release Preparation.

## 🔴 Open Gaps & Unfinished Integration Points
*   *(No high-priority open gaps identified for this cycle)*

---

## 💡 Strategic Simplification Ideas (Ideas: 55)
*   **SIMP-OCT20.22-S1: Automated Version Injector for Tests (Low).** Consider using `BuildConfig` values directly in unit tests to avoid manual version string updates during baseline synchronization.
*   **SIMP-OCT20.21-S1: Unified Forensic Writer (Low).** Consider overloading `writeTrace` to accept a primitive parameter set directly, reducing the need for two distinct internal paths in `ForensicSpillBuffer`.
*   **SIMP-OCT20.10-S1: Test-Only Log Buffer Draining (Low).** Consider adding a `isManualFlushingOnly` flag to `LogRepository` during tests to completely inhibit the background `runBatchProcessor` without needing to cancel jobs, further reducing race risks.
*   **SIMP-OCT20.7-S1: Automated Documentation Sharding (Low).** Consider implementing a build-time check or script that automatically shards `.md` files into archives when they exceed a specific line count (e.g., 500 lines).
*   **SIMP-OCT20.2-S1: Deterministic Audit Verification (Low).** Consider integrating `logRepository.flushLogs()` into a base test rule to automatically ensure deterministic persistence.
*   **SIMP-OCT19.20-S1: Automated Doc-Sync Hook (Low).** Consider implementing a pre-commit hook that verifies `SOT_MASTER_REQUIREMENTS.md` length and header count against `RESOLUTION_ARCHIVE.md`.
*   **SIMP-OCT19.19-S1: Unified Restoration Policy (Low).** Move restoration bypass logic from `MainViewModel` into `SessionUseCase`.
*   **SIMP-OCT18.24-S1: Reactive Signaling Deferral (Low).** Unify `shouldDeferSignaling` check into a single `Flow<Boolean>`.
*   **SIMP-OCT18.18-S1: Manual Processor Reification (Low).** Formalize `triggerManualBatchProcessor` into a `TestingCommand` stream.
*   **SIMP-OCT18.16-S1: Unified Power Intent Handler (Low).** Consolidate power/idle intent receivers in `AndroidPowerStateProvider`.
*   **SIMP-OCT18.14-S1: Altitude-Baro Fusion for Anchors (Medium).** Use barometric trends to validate GNSS altitude shifts.
*   **SIMP-OCT18.12-S1: Acoustic State Machine (Medium).** Refactor duty-cycle logic in `HardwareSuite.kt` into a state machine.
*   **SIMP-OCT18.10-S1: PID-Controlled Vibration Floor (Medium).** Replace multi-stage EMA with a simple PID controller.
*   **SIMP-OCT18.8-S1: SNR-Aware Kalman Gain (Medium).** Dynamically scale process noise based on SNR stability.
*   **SIMP-OCT18.4-S1: Unified Trace Header (Low).** Consolidate `LogEntry` and `ForensicSnapshot` for JNI transfers.
*   **SIMP-OCT18.0-S1: JNI Buffer Validator (Low).** Add startup-time JNI verification for `sharedStateBuffer` structures.
*   **SIMP-OCT17.11-S2: Unified JNI Offset Mapping (Medium).** Centralize shared memory offsets.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT20.22-F1: Test Suite Version Regression.** Resolved Oct20.22. Fixed `MainViewModelMappingTest` to align with the `Oct20.21` engine version baseline, ensuring full suite passability. (SOT ID 783).
*   **ISSUE-OCT20.10-F1: Forensic Buffer Overflow Handling.** Resolved Oct20.21. Hardened `ForensicSpillBuffer` against extreme overflow scenarios by implementing a circular overwrite policy to ensure zero data loss of recent traces. (SOT ID 782).
*   **ISSUE-OCT18.22-F3: Storage Test Race Conditions.** Resolved Oct20.10. Eliminated race conditions in `StoragePressureAuditTest` by disabling background batch processing during deterministic flushing and adding async propagation delays for trail points. (SOT ID 781).
*   **ISSUE-OCT20.7-F1: Forensic Document Archival.** Resolved Oct20.7. Optimized documentation by migrating legacy SOT IDs and resolutions to `docs_history_archive.md`. (SOT ID 780).
*   **ISSUE-OCT20.3-F1: Hardware Profile Test Regression.** Resolved Oct20.3. Fixed compilation failure in `HardwareSuiteProfileTest.kt`. (SOT ID 779).
*   **ISSUE-OCT19.0-F1: Viewer with Simulated Tracker.** Resolved Oct20.2. Hardened mode restoration bypass in `MainViewModel`. (SOT ID 778).
*   **ISSUE-OCT20.0-F2: Testing Guideline Hardening.** Resolved Oct20.0. Updated protocol to enforce forensic preservation. (SOT ID 777).
*   **ISSUE-OCT20.0-F1: Document Integrity Baseline.** Resolved Oct20.0. Established versioned baseline directory. (SOT ID 775).

---

## 📊 Hardening Progress Dashboard
- **Oct20.22: [SOT Count: 783 (Rules: 302), Open: H:0, M:0, L:0, Ideas: H:1, M:19, L:35, Testing: 103 (Sub-items: 104), QA: 354]**
- **Oct20.21: [SOT Count: 782 (Rules: 302), Open: H:0, M:0, L:0, Ideas: H:1, M:19, L:34, Testing: 102 (Sub-items: 103), QA: 353]**
- **Oct20.10: [SOT Count: 781 (Rules: 301), Open: H:0, M:0, L:0, Ideas: H:1, M:19, L:33, Testing: 101 (Sub-items: 102), QA: 352]**
- **Oct20.7: [SOT Count: 780 (Rules: 301), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:32, Testing: 100 (Sub-items: 101), QA: 351]**
- **Oct20.3: [SOT Count: 779 (Rules: 301), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:31, Testing: 100 (Sub-items: 101), QA: 350]**
- **Oct20.2: [SOT Count: 778 (Rules: 301), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:31, Testing: 100 (Sub-items: 101), QA: 349]**
- **Oct20.0: [SOT Count: 777 (Rules: 299), Open: H:0, M:0, L:1, Ideas: H:1, M:19, L:30, Testing: 100 (Sub-items: 101), QA: 348]**
