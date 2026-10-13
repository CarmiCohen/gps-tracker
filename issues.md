# Project Issues & Hardening Tracking (Rigorous Audit) - Oct13.20

## 🎯 Current Resumption Focus: GPS Engine Reliability.

## 🛠️ Open Gaps & Unfinished Integration Points

### 🔴 High Priority (0)
*   **ISSUE-OCT13.20-H1: Delicate API Usage (Resolved).** `SmartSignalingDispatcher.kt` transitioned to managed coroutine jobs and robust exception handling for channel closure. Resolved Oct13.20.

### 🟡 Medium Priority (0)
*   **ISSUE-OCT13.20-M1: Manifest Audit (Resolved).** Removed `android:extractNativeLibs` from `AndroidManifest.xml`. Resolved Oct13.20.
*   **ISSUE-OCT13.20-M2: API Deprecations (Resolved).** 
    *   Migrated `VIBRATOR_SERVICE` to `VibratorManager` on API 31+.
    *   Migrated Icons (`ArrowBack`, `List`, `VolumeUp`) to `AutoMirrored` versions.
    *   Migrated `LocalLifecycleOwner` to `androidx.lifecycle.compose`. Resolved Oct13.20.

### 🔵 Low Priority (1)
*   **ISSUE-OCT13.20-L1: Dead Code & Hygiene Cleanup.** 
    *   ~50 unused parameters/variables across `core:engine` and `app`. (Partial cleanup in `LocationProcessor.kt` completed).
    *   Redundant cast in `MapOverlayManager.kt:241`.
    *   Gradle 9.0 compatibility warnings.

---

## 💡 Strategic Simplification Ideas (Ideas: 19)
*   **SIMP-OCT13.20-S1: Signaling Payload Conflation Logic (Low).** The conflation logic in `SmartSignalingDispatcher` for Maps vs Objects is very similar. Could be unified into a single generic strategy to reduce logic duplication.
*   **SIMP-OCT13.8-S1: Revival-Trigger Consolidation (Low).** Consider merging `gpsRestartTrigger` and `revivalPulseJob` into a unified provider lifecycle manager to reduce redundant restart logic.
*   **SIMP-OCT13.7-S1: Base Isolated Repository (Low).** Standardize background loop management (Job tracking, `isTestMode`, `resetForTest`) into a base class to reduce boilerplate and ensure consistent test isolation across all repositories.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT13.20-H1: Signaling Lifecycle Hardening.** Replaced delicate API usages with robust job-managed transitions. Resolved Oct13.20.
*   **ISSUE-OCT13.20-M1: Manifest Audit.** Resolved Oct13.20.
*   **ISSUE-OCT13.20-M2: API Modernization.** Resolved Oct13.20.
*   **SIMP-OCT12.18-S1: Pruning Scheduler.** Fully migrated background maintenance to `PruningWorker` (WorkManager) with `DeviceIdle` constraints. Resolved Oct13.8.
*   **ISSUE-OCT12.13-1: UI Staleness / Signal Loss.** Increased `VIEWER_SIGNAL_LOSS_THRESHOLD_MS` to 90s. Resolved Oct13.8.

---

## 📊 Hardening Progress Dashboard
- **Oct13.20: [SOT Count: 403, Open: H:0, M:0, L:1, Ideas: H:0, M:9, L:10, QA: 303]**
- **Oct10.3: [SOT Count: 405 (Rules: 251), Open: H:0, M:0, L:0, Ideas: H:0, M:9, L:9, Testing: 10 (Sub-items: 46), QA: 305]**
- **Oct13.7: [SOT Count: 402 (Rules: 248), Open: H:0, M:0, L:0, Ideas: H:1, M:9, L:8, Testing: 10 (Sub-items: 46), QA: 302]**
- **Oct13.5: [SOT Count: 396 (Rules: 243), Open: H:1, M:0, L:0, Ideas: H:1, M:9, L:7, Testing: 8 (Sub-items: 40), QA: 298]**
