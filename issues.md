# Project Issues & Hardening Tracking (Sep.08.13)

## 🎯 Current Resumption Focus: Structural Refinement
Issue #924 and R-ID 259 resolved. Next focus: Decomposing bloated state models.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Telemetry Data Class Flattening**: (Idea #2 from `Simplify_Ideas2.md`). `LocationUpdate` exceeds 70 fields. Partition into `KineticState`, `AtmosphericState`, and `IntegrityState` to reduce allocation churn.
*   **Alarm Logic Partitioning**: (Idea #3 from `Simplify_Ideas2.md`). Partition `evaluateAlarms` in `AlarmManager` into specialized evaluators (Physical, Health, Connectivity) to reduce cyclomatic complexity.

## 🟢 Recently Resolved Issues (Sep.08.13)
*   **Issue #924 RESOLVED: Visibility Hardening**. Added "Safe Mode" (SAF) and "GNSS Throttled" (THR) status indicators to HUD and Dashboard for A15 Hysteresis transparency (R-ID 267).
*   **R-ID 259 RESOLVED: Energy Audit Integration**. Mapped `ForensicAuditor` energy verdicts (Delta mA, Temp Rise, Duration) to `SystemHealthState` and `DiagnosticState` for structured UI reporting.

## 🟢 Recently Resolved Issues (Sep.08.12)
*   **Issue #936 RESOLVED: Forensic Auditor Consolidation (Idea #3)**. Extracted shared audit logic (Reliability/Jitter) from `TrackerService` and `ViewerService` into a unified `ForensicAuditor` (R-ID 280).
*   **Issue #910 HARDENED: Hydration Watchdog Active Recovery**. Implemented forced re-hydration path for stalls at Level 2 (R-ID 281).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 298 (Rules: 53, IDs: 245), Resolved: 947, Open: 2, Testing: 95% (Sub-items: 48), Ideas: 4, QA: 267]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.08.13)*
