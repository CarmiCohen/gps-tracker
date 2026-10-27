git add .
git commit -m "Fix: Forensic SPI Level Audit and Staggered Persistence. #ISSUE-OCT27.7-F1"
git tag -a Oct27.5 -m "Oct27.5 Release: SPI Bus Contention Mitigation"
git push origin Oct27.5

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct27.5

## 🎯 Current Resumption Focus: Forensic SPI Level Audit.

## 🔴 Open Gaps & Unfinished Integration Points
*   *None.*

---

## 💡 Strategic Simplification Ideas (Ideas: 102)
*   **SIMP-OCT27.7-S1: IO Latency-Driven Throttle (Low).** Consider dynamically increasing the forensic drain interval if `maxIoLatency` stays above 500ms for sustained periods, even if fill level is high.
*   **SIMP-OCT27.6-S2: Database Recovery Lock (Medium).** Consider using a dedicated lock file or SQLite transaction state to prevent LogRepository from starting standard drainers until the Forensic Recovery phase is fully committed.
*   **SIMP-OCT27.6-S1: Recovery Buffer Over-Provisioning (Low).** Evaluate if `ForensicSpillBuffer` capacity can be dynamically scaled based on storage health to prevent circular overwrite during long-duration offline events.
*   **SIMP-OCT27.5-S1: Badge Measurement Caching (Low).** Investigate if `MeasurableBadge` can benefit from `Modifier.layout` caching to further optimize rapid-fire HUD updates during high-pressure signaling.
*   **SIMP-OCT27.4-S1: Test Application Base Class (Medium).** Consolidate `GpsApplication` and `GpsTestBaseApplication` into a shared `BaseGpsApplication` to reduce duplication of WorkManager configuration logic.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT27.7-F1: Forensic SPI Level Audit.** Resolved Oct27.5. Mitigated SPI bus contention by implementing staggered `force()` logic and transitioning persistence operations to `AuditType.IO` for system health tracking. (Resolved GAP-OCT27.6-1).
*   **ISSUE-OCT27.6-F1: Forensic Recovery Baseline.** Resolved Oct27.5. Established `ForensicRecoveryAuditTest` and verified zero-loss telemetry during simulated process restarts. (Resolved GAP-OCT27.5-1).
*   **ISSUE-OCT27.5-F2: Instrumented Test Compilation Failure.** Resolved Oct27.5. Refactored `TestApplication.kt` to use `EntryPoint` for `HiltWorkerFactory`, circumventing Hilt restrictions on `@Inject` fields in custom test app base classes.
*   **ISSUE-OCT27.5-F1: Unit-Based Badge Scaling.** Resolved Oct27.5. Implemented `MeasurableBadge` shared component and refactored HUD indicators to reduce redundant Compose declarations.
*   **ISSUE-OCT27.4-F1: PruningWorker Initialization Failure in Tests.** Resolved Oct27.5. Integrated `HiltWorkerFactory` into `GpsTestBaseApplication`.

---

## 📊 Hardening Progress Dashboard
- **Oct27.5: [SOT Count: 853 (Rules: 354), Open: H:0, M:0, L:0, Ideas: 102, Testing: 1 (Sub-items: 39), QA: PASSED]**
- **Oct27.5: [SOT Count: 852 (Rules: 353), Open: H:0, M:0, L:0, Ideas: 101, Testing: 1 (Sub-items: 38), QA: PASSED]**
- **Oct27.5: [SOT Count: 851 (Rules: 352), Open: H:1, M:0, L:0, Ideas: 99, Testing: FAILED (Build Error), QA: PENDING]**
