# Project Issues & Hardening Tracking (Rigorous Audit) - Oct14.1

## 🎯 Current Resumption Focus: Full Project Test Cycle & Hardening Verification

## 🔴 Open Gaps & Unfinished Integration Points
*   (None) - All high-priority telemetry issues resolved in Oct14.1.

---

## 💡 Strategic Simplification Ideas (Ideas: 34)
*   **SIMP-OCT16.10-S1: JNI Buffer Schema Generator (High).** Create a simple script to generate both Kotlin `ByteBuffer` offsets and C++ `struct` offsets from a single YAML definition to prevent future alignment regressions.
*   **SIMP-OCT16.2-S1: Repository State Projection (Low).** Consider extracting a `StateProvider` interface from `MainRepository` to allow UseCases to observe specific sub-states without full repository access.
*   **SIMP-OCT16.9-S1: Unified Violation Evaluation (Medium).** Consolidate `isTracker` guards into a `ViolationEvaluator` that understands if the source data is local or remote, instead of hardcoding role checks in `MainAlarmLogic`.
*   **SIMP-OCT16.9-S2: Vibration EMA Boundary Check (Low).** Add `isNaN` guards to `SentinelForensicState` updates to prevent telemetry poisoning.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT16.9-F2: Regression: Vibration Telemetry NaN in HUD.** Resolved HUD `NaNG` by hardening JNI `n19` with `isnan()` guards and enforcing strict 8-byte alignment for 64-bit primitives in the shared buffer. (R-ID 602).
*   **ISSUE-OCT16.9-F1: Regression: Viewer Remote Alarm Authority Broken.** Fixed regression where `isTracker` guards prevented Viewers from siren-alerting for remote physical violations. Verified via `AlarmStealthTest`.
*   **ISSUE-OCT16.8-F1: Missing Termination Confirmation Dialog.** Implemented `DialogHost` in `MainAppContent.kt` to ensure termination dialogs are not obscured by overlays. (SIMP-OCT16.8-S1)
*   **ISSUE-OCT16.8-F2: Role Transition Grace Period Violation.** Added 30s hardware boot grace to `AppAlarmManager` and fixed `isTrackerMode` atomic synchronization.
*   **SOT ID 749: Project-Wide Hardening Audit (Oct14.1).** Verified Chapter 3 (Tracker Mode Operation) on physical A15 hardware. Confirmed stealth sentinel, service persistence, and HUD logic.
*   **SOT ID 748: Test State Pre-warming.** Formalized global state reset.
*   **ISSUE-OCT16.2-F1: Hydration Test Timeout.** Fixed OSM readiness stall.
*   **SIMP-OCT15.10-S1: Test Utility Decoupling.** Decoupled `MainFileHelper` from `MainViewModel`.

---

## 📊 Hardening Progress Dashboard
- **Oct14.1: [SOT Count: 445 (Rules: 283), Open: H:0, M:0, L:0, Ideas: H:1, M:12, L:21, Testing: 22 (Sub-items: 160), QA: 361]**
- **Oct14.1: [SOT Count: 444 (Rules: 282), Open: H:1, M:0, L:0, Ideas: H:0, M:12, L:21, Testing: 22 (Sub-items: 160), QA: 360]**
- **Oct14.1: [SOT Count: 444 (Rules: 282), Open: H:0, M:0, L:0, Ideas: H:0, M:11, L:21, Testing: 22 (Sub-items: 160), QA: 360]**
