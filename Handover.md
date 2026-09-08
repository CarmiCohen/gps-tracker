# Forensic Handover (Sep.08.12)

## 🎯 Current Context: Issue #936 & #910 Resolution
The stability audit logic (Reliability % and GNSS Jitter) has been fully consolidated into `ForensicAuditor`, restoring SRP to the background services. Additionally, the `Hydration Watchdog` in `MainViewModel` now implements active recovery, forcing a re-hydration of the UI if a Level 2 stall is detected.

## 🛠️ Key Changes
*   **ForensicAuditor.kt**: Unified `recordGpsFix` and `evaluateStability` logic; exposed `lastGpsFixRealtime`.
*   **TrackerService.kt / ViewerService.kt**: Removed local audit state variables; delegated stability reporting to the auditor.
*   **MainViewModel.kt**: Added recovery branch to the Hydration Watchdog to reset and restart `hydrationManager`.
*   **app/build.gradle**: Updated version to `Sep.08.12`.
*   **SOT**: Added R-ID 280 (Consolidation) and R-ID 281 (Hydration Recovery).

## 📡 Next Priority
*   **Issue #924 Visibility**: Add "Safe Mode" and "GNSS Throttled" (A15 Hysteresis) status indicators to the HUD.
*   **Energy Audit Integration**: Propagate `ForensicAuditor` energy verdicts to the UI log.

## 📊 Dashboard Snapshot
- **Current Audit Baseline: [SOT: 296 (Rules: 53, IDs: 243), Resolved: 945, Open: 2, Testing: 95% (Sub-items: 48), Ideas: 4, QA: 266]**
