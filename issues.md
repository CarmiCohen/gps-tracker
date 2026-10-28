git add .
git commit -m "Fix: Latency EMA Smoothing for Throttle. #ISSUE-OCT27.10-S1"
git tag -a Oct27.10 -m "Oct27.10 Release: Latency EMA Smoothing"
git push origin Oct27.10

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct27.10

## 🎯 Current Resumption Focus: Strategic Simplification.

## 🔴 Open Gaps & Unfinished Integration Points
*   *None.*

---

## 💡 Strategic Simplification Ideas (Ideas: 102)
*   **SIMP-OCT27.10-S1: Pruning Batch Optimization (Low).** Consider dynamic adjustment of `REFINED_PRUNE_CHUNK_SIZE` based on the delta between current size and target threshold to speed up deep prunes.
*   **SIMP-OCT27.8-S1: Database Transaction Timeout (Low).** Consider adding a watchdog to the `recoveryLock` to prevent permanent stalls if a recovery transaction hangs indefinitely.
*   **SIMP-OCT27.6-S1: Recovery Buffer Over-Provisioning (Low).** Evaluate if `ForensicSpillBuffer` capacity can be dynamically scaled based on storage health to prevent circular overwrite during long-duration offline events.
*   **SIMP-OCT27.5-S1: Badge Measurement Caching (Low).** Investigate if `MeasurableBadge` can benefit from `Modifier.layout` caching to further optimize rapid-fire HUD updates during high-pressure signaling.
*   **SIMP-OCT27.4-S1: Test Application Base Class (Medium).** Consolidate `GpsApplication` and `GpsTestBaseApplication` into a shared `BaseGpsApplication` to reduce duplication of WorkManager configuration logic.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT27.10-S1: Latency EMA Smoothing.** Resolved Oct27.10. Implemented Exponential Moving Average (EMA) for `maxIoLatency` in the forensic drainer to prevent throttle oscillation during transient storage spikes (R-ID 752).
*   **ISSUE-OCT27.9-F1: IO Latency-Driven Throttle.** Resolved Oct27.10. Implemented dynamic scaling of the forensic drain interval and batch size based on real-time `maxIoLatency` feedback to mitigate storage contention (R-ID 751).
*   **ISSUE-OCT27.8-F1: Database Recovery Lock.** Resolved Oct27.10. Implemented `recoveryLock` (Mutex) in `LogRepository` to ensure exclusive DB access during the forensic recovery phase, preventing race conditions with standard drainers and maintenance workers (R-ID 750).
*   **ISSUE-OCT27.7-F1: Forensic SPI Level Audit.** Resolved Oct27.10. Mitigated SPI bus contention by implementing staggered `force()` logic and transitioning persistence operations to `AuditType.IO` for system health tracking. (Resolved GAP-OCT27.6-1).
*   **ISSUE-OCT27.6-F1: Forensic Recovery Baseline.** Resolved Oct27.10. Established `ForensicRecoveryAuditTest` and verified zero-loss telemetry during simulated process restarts. (Resolved GAP-OCT27.5-1).
*   **ISSUE-OCT27.5-F2: Instrumented Test Compilation Failure.** Resolved Oct27.10. Refactored `TestApplication.kt` to use `EntryPoint` for `HiltWorkerFactory`, circumventing Hilt restrictions on `@Inject` fields in custom test app base classes.
*   **ISSUE-OCT27.5-F1: Unit-Based Badge Scaling.** Resolved Oct27.10. Implemented `MeasurableBadge` shared component and refactored HUD indicators to reduce redundant Compose declarations.
*   **ISSUE-OCT27.4-F1: PruningWorker Initialization Failure in Tests.** Resolved Oct27.10. Integrated `HiltWorkerFactory` into `GpsTestBaseApplication`.

---

## 📊 Hardening Progress Dashboard
- **Oct27.10: [SOT Count: 856 (Rules: 357), Open: H:0, M:0, L:0, Ideas: 102, Testing: 1 (Sub-items: 42), QA: PASSED]**
- **Oct27.10: [SOT Count: 855 (Rules: 356), Open: H:0, M:0, L:0, Ideas: 102, Testing: 1 (Sub-items: 41), QA: PASSED]**
- **Oct27.10: [SOT Count: 854 (Rules: 355), Open: H:0, M:0, L:0, Ideas: 102, Testing: 1 (Sub-items: 40), QA: PASSED]**
