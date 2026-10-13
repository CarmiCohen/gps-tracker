# Project Issues & Hardening Tracking (Rigorous Audit) - Oct.13.8

## 🎯 Current Resumption Focus: GPS Engine Reliability.

## 🛠️ Open Gaps & Unfinished Integration Points

### 🔴 High Priority (0)

### 🟡 Medium Priority (0)

### 🔵 Low Priority (0)

---

## 💡 Strategic Simplification Ideas (Ideas: 18)
*   **SIMP-OCT13.8-S1: Revival-Trigger Consolidation (Low).** Consider merging `gpsRestartTrigger` and `revivalPulseJob` into a unified provider lifecycle manager to reduce redundant restart logic.
*   **SIMP-OCT13.7-S1: Base Isolated Repository (Low).** Standardize background loop management (Job tracking, `isTestMode`, `resetForTest`) into a base class to reduce boilerplate and ensure consistent test isolation across all repositories.
*   **SIMP-OCT13.5-S1: Test-Only Persistence Gating (Resolved).** Implemented `isTestMode` in `LogRepository`.

---

## ✅ Resolved Traceability & Metadata Issues
*   **SIMP-OCT12.18-S1: Pruning Scheduler.** Fully migrated background maintenance to `PruningWorker` (WorkManager) with `DeviceIdle` constraints. Resolved Oct13.8.
*   **ISSUE-OCT12.13-1: UI Staleness / Signal Loss.** Increased `VIEWER_SIGNAL_LOSS_THRESHOLD_MS` to 90s to accommodate 45s Screen-Off polling and budget network jitter. Resolved Oct13.8.
*   **SIMP-OCT12.17-S1: Periodic GPS Re-Zeroing.** Implemented reactive `gpsRestartTrigger` in `HardwareSuite.kt` to silently restart GPS provider every 6h if accuracy > 30m. Resolved Oct13.8.
*   **ISSUE-OCT13.5-3: Forensic Test isolation & Drain Deficit.** Resolved by aggressive test-mode draining and `PeekResult` pointer advancement. Resolved Oct13.7.
*   **ISSUE-OCT13.6-1: Suspendable Task Management.** Made `stopBackgroundTasks` and `resetForTest` suspendable with `cancelAndJoin` to guarantee isolation. Resolved Oct13.7.
*   **ISSUE-OCT13.5-2: Forensic Bit-Overflow Fix.** Resolved bit-packing collision in `LogRepository.kt`. Resolved Oct13.5.
*   **ISSUE-OCT13.5-1: OfflineRepository Extreme Load Test.** Resolved Oct13.5.

---

## 📈 Hardening Progress Dashboard
- **Oct.13.8: [SOT Count: 405 (Rules: 251), Open: H:0, M:0, L:0, Ideas: H:0, M:9, L:9, Testing: 10 (Sub-items: 46), QA: 305]**
- **Oct13.7: [SOT Count: 402 (Rules: 248), Open: H:0, M:0, L:0, Ideas: H:1, M:9, L:8, Testing: 10 (Sub-items: 46), QA: 302]**
- **Oct13.5: [SOT Count: 396 (Rules: 243), Open: H:1, M:0, L:0, Ideas: H:1, M:9, L:7, Testing: 8 (Sub-items: 40), QA: 298]**
