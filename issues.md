# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

---

## 🟢 Resolved Traceability & Metadata Issues (Audit: Sep.17.04)

1. **Issue #1094: Forensic Backfill Buffer Reuse Optimization**
    *   *Finding*: `HistoryManager.backfillAnalyticalGaps` creates a new `ArrayList` and multiple `ConnectionPoint` objects on every call, leading to transient heap pressure during recovery bursts.
    *   *Action*: Implement a reusable pre-allocated buffer and a flyweight pool for `ConnectionPoint` objects within `HistoryManager` to eliminate allocations during backfill operations.
    *   *Status*: **Resolved**.

2. **Issue #1093: Power & Hardware Provider Convergence**
    *   *Finding*: `UnifiedPowerPolicy` and `HardwareProvider` shared overlapping responsibilities regarding platform state monitoring and signaling backoff.
    *   *Action*: Merged both components into `HardwareSuite.kt`. Updated all dependent services (`TrackerService`, `ViewerService`, `BaseMonitorService`) and managers (`ConnectivitySuite`, `IntegrityMonitor`, `HistoryManager`) to utilize the unified suite.
    *   *Status*: **Resolved**.

3. **Issue #1074: Telemetry Backfill QA Task**
    *   *Finding*: QA Verification of telemetry backfill convergence (R-ID 17 / Signaling Continuity).
    *   *Action*: Added comprehensive QA verification unit tests within `TelemetryAggregatorTest.kt` to enforce zero-churn telemetry alignment, gap processing bounds, and `MAX_BACKFILL_POINTS` cap validation.
    *   *Status*: **Resolved**.

4. **Issue #1073: Event Log Erasure Defect**
    *   *Finding*: Erasing the event log button does not erase the log.
    *   *Action*: Handled `UiEvent.ClearLogs` explicitly inside `MainViewModel.kt` to trigger `repository.clearLogs()`.
    *   *Status*: **Resolved**.

5. **Issue #1051: Signaling Conflation Traceability**
    *   *Finding*: `CommunicationManager.kt` utilized hardcoded literals (100ms/20ms) in `emitLocationConflated`.
    *   *Action*: Migrated these to `SIGNALING_CONFLATION_DELAY_MS` and `SIGNALING_CONFLATION_DELAY_VIOLATION_MS` in `EngineConstants.kt` to comply with R312.
    *   *Status*: **Resolved**.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 354 (Rules: 71, IDs: 354), Resolved: 1094, Open: 0, Testing: 2, Ideas: 18, QA: 281]**
