git add .
git commit -m "Fix: Settings Restoration Policy. #SIMP-OCT28.12-S1"
git tag -a Oct28.13 -m "Oct28.13 Release: Settings Restoration Policy"
git push origin Oct28.13

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct28.13

## 🎯 Current Resumption Focus: Strategic Simplification.

## 🔴 Open Gaps & Unfinished Integration Points
*   *None.*

---

## 💡 Strategic Simplification Ideas (Ideas: 99)
*   **SIMP-OCT27.5-S1: Badge Measurement Caching (Low).** Investigate if `MeasurableBadge` can benefit from `Modifier.layout` caching to further optimize rapid-fire HUD updates during high-pressure signaling.

---

## ✅ Resolved Traceability & Metadata Issues
*   **SIMP-OCT28.12-S1: Settings Restoration Policy.** Resolved Oct28.13. Implemented a factory reset fallback in `SettingsRepository` that triggers after 3 consecutive integrity signature failures. This ensures the app can recover from persistent configuration corruption (R-ID 759).
*   **SIMP-OCT28.11-S1: Settings Invalidation Guard.** Resolved Oct28.12. Implemented a SHA-256 integrity guard for `SettingsRepository` to detect and handle data corruption during OS-level backup/restore events or unexpected process deaths (R-ID 758).
*   **SIMP-OCT27.6-S1: Recovery Buffer Over-Provisioning.** Resolved Oct27.10. Implemented dynamic capacity scaling for `ForensicSpillBuffer` (2,000 to 50,000 entries) based on available storage health to prevent over-provisioning while maximizing retention on high-capacity hardware (R-ID 757).
*   **SIMP-OCT28.2-S1: Pruning Telemetry Aggregation.** Resolved Oct27.10. Implemented rolling average (EMA) of purged record counts in `MainRepository` and `SystemHealthState` to visualize maintenance efficiency over time (R-ID 756).
*   **SIMP-OCT27.8-S1: Database Transaction Timeout.** Resolved Oct27.10. Implemented watchdog mechanism using `withTimeout(DATABASE_TRANSACTION_TIMEOUT_MS)` on all recoveryLock acquisitions in `LogRepository` to prevent permanent stalls (R-ID 755).
*   **SIMP-OCT28.0-S1: Pruning Worker Telemetry.** Resolved Oct27.10. Implemented reporting of total purged record counts per maintenance cycle to telemetry and log repository audits (R-ID 754).
*   **SIMP-OCT27.4-S1: Test Application Base Class.** Resolved Oct27.10. Consolidated `GpsApplication` and `GpsTestBaseApplication` into a shared `BaseGpsApplication` to reduce duplication of WorkManager configuration logic (R-ID 745).
*   **SIMP-OCT27.10-S1: Pruning Batch Optimization.** Resolved Oct27.10. Implemented dynamic adjustment of pruning chunk sizes based on backlog pressure to accelerate storage recovery during deep maintenance cycles (R-ID 753).
*   **ISSUE-OCT27.10-S1: Latency EMA Smoothing.** Resolved Oct27.10. Implemented Exponential Moving Average (EMA) for `maxIoLatency` in the forensic drainer to prevent throttle oscillation during transient storage spikes (R-ID 752).
*   **ISSUE-OCT27.9-F1: IO Latency-Driven Throttle.** Resolved Oct27.10. Implemented dynamic scaling of the forensic drain interval and batch size based on real-time `maxIoLatency` feedback to mitigate storage contention (R-ID 751).
*   **ISSUE-OCT27.8-F1: Database Recovery Lock.** Resolved Oct27.10. Implemented `recoveryLock` (Mutex) in `LogRepository` to ensure exclusive DB access during the forensic recovery phase, preventing race conditions with standard drainers and maintenance workers (R-ID 750).
*   **ISSUE-OCT27.7-F1: Forensic SPI Level Audit.** Resolved Oct27.10. Mitigated SPI bus contention by implementing staggered `force()` logic and transitioning persistence operations to `AuditType.IO` for system health tracking. (Resolved GAP-OCT27.6-1).
*   **ISSUE-OCT27.6-F1: Forensic Recovery Baseline.** Resolved Oct27.10. Established `ForensicRecoveryAuditTest` and verified zero-loss telemetry during simulated process restarts. (Resolved GAP-OCT27.5-1).
*   **ISSUE-OCT27.5-F2: Instrumented Test Compilation Failure.** Resolved Oct27.10. Refactored `TestApplication.kt` to use `EntryPoint` for `HiltWorkerFactory`, circumventing Hilt restrictions on `@Inject` fields in custom test app base classes.
*   **ISSUE-OCT27.5-F1: Unit-Based Badge Scaling.** Resolved Oct27.10. Transitioned HUD status badges to a unified `MeasurableBadge` architecture (R-ID 747).

---

## 📊 Hardening Progress Dashboard
- **Oct28.13: [SOT Count: 864 (Rules: 364), Open: H:0, M:0, L:0, Ideas: 99, Testing: 1 (Sub-items: 48), QA: PASSED]**
- **Oct28.12: [SOT Count: 863 (Rules: 363), Open: H:0, M:0, L:0, Ideas: 99, Testing: 1 (Sub-items: 48), QA: PASSED]**
- **Oct27.10: [SOT Count: 862 (Rules: 362), Open: H:0, M:0, L:0, Ideas: 98, Testing: 1 (Sub-items: 47), QA: PASSED]**
