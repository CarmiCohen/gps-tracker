git add .
git commit -m "Remediate: Alarm Leakage on Tracker. #1391"
git tag -a Oct27.10 -m "Oct27.10 Release: Stealth Hardening"
git push origin Oct27.10

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct27.10

## 🎯 Current Resumption Focus: Strategic Simplification.

## 🔴 Open Gaps & Unfinished Integration Points
*   *None.*

---

## 💡 Strategic Simplification Ideas (Ideas: 100)
*   **SIMP-OCT28.15-S1: CompositionLocal Gating (Medium).** Evaluate if `LocalAnimationGating` can be further utilized to suppress non-critical layout passes in `MeasurableBadge` during extreme thermal pressure.
*   **SIMP-OCT28.16-S1: Audio Suppression Centralization (Low).** Consolidate `isTrackerMode` checks into a single `StealthProvider` to avoid redundant injections in `AudioSynthesizer` and `UiEventCoordinator`.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT28.16-F1: Alarm Leakage on Tracker.** Resolved Oct27.10. Implemented role-based suppression in `AudioSynthesizer`, `UiEventCoordinator`, and `AlarmOverlayService` to enforce absolute stealth on Trackers (R872 / Issue #1391).
*   **SIMP-OCT27.5-S1: Badge Measurement Caching.** Resolved Oct27.10. Optimized `MeasurableBadge` using custom `Layout` flattening to reduce measurement overhead and hierarchy depth (R-ID 760).
*   **SIMP-OCT28.12-S1: Settings Restoration Policy.** Resolved Oct27.10. Implemented a factory reset fallback in `SettingsRepository` that triggers after 3 consecutive integrity signature failures (R-ID 759).
*   **SIMP-OCT28.11-S1: Settings Invalidation Guard.** Resolved Oct27.10. Implemented a SHA-256 integrity guard for `SettingsRepository` to detect and handle data corruption (R-ID 758).
*   **SIMP-OCT27.6-S1: Recovery Buffer Over-Provisioning.** Resolved Oct27.10. Implemented dynamic capacity scaling for `ForensicSpillBuffer` (R-ID 757).
*   **SIMP-OCT28.2-S1: Pruning Telemetry Aggregation.** Resolved Oct27.10. Implemented rolling average (EMA) of purged record counts (R-ID 756).
*   **SIMP-OCT27.8-S1: Database Transaction Timeout.** Resolved Oct27.10. Implemented watchdog mechanism using `withTimeout` on all recoveryLock acquisitions (R-ID 755).
*   **SIMP-OCT28.0-S1: Pruning Worker Telemetry.** Resolved Oct27.10. Implemented reporting of total purged record counts per maintenance cycle (R-ID 754).
*   **SIMP-OCT27.4-S1: Test Application Base Class.** Resolved Oct27.10. Consolidated application base classes into `BaseGpsApplication` (R-ID 745).
*   **SIMP-OCT27.10-S1: Pruning Batch Optimization.** Resolved Oct27.10. Implemented dynamic adjustment of pruning chunk sizes (R-ID 753).
*   **ISSUE-OCT27.10-S1: Latency EMA Smoothing.** Resolved Oct27.10. Implemented EMA for `maxIoLatency` in the forensic drainer (R-ID 752).
*   **ISSUE-OCT27.9-F1: IO Latency-Driven Throttle.** Resolved Oct27.10. Implemented dynamic scaling based on real-time `maxIoLatency` (R-ID 751).
*   **ISSUE-OCT27.8-F1: Forensic Recovery Lock.** Resolved Oct27.10. Implemented `recoveryLock` in `LogRepository` (R-ID 750).
*   **ISSUE-OCT27.7-F1: Forensic SPI Level Audit.** Resolved Oct27.10. Mitigated SPI bus contention with staggered `force()` logic.
*   **ISSUE-OCT27.6-F1: Forensic Recovery Baseline.** Resolved Oct27.10. Established `ForensicRecoveryAuditTest` and verified zero-loss telemetry.
*   **ISSUE-OCT27.5-F2: Instrumented Test Compilation Failure.** Resolved Oct27.10. Refactored `TestApplication.kt` for Hilt compatibility.
*   **ISSUE-OCT27.5-F1: Unit-Based Badge Scaling.** Resolved Oct27.10. Transitioned HUD status badges to a unified `MeasurableBadge` architecture (R-ID 747).

---

## 📊 Hardening Progress Dashboard
- **Oct27.10: [SOT Count: 866 (Rules: 366), Open: H:0, M:0, L:0, Ideas: 100, Testing: 1 (Sub-items: 50), QA: PASSED]**
- **Oct27.10: [SOT Count: 865 (Rules: 365), Open: H:0, M:0, L:0, Ideas: 99, Testing: 1 (Sub-items: 49), QA: PASSED]**
