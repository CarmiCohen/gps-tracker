# Project Issues & Hardening Tracking (Sep.08.12)

## 🎯 Current Resumption Focus: UI Visibility for Safe Mode
Implement status indicators for Safe Mode and A15 Hysteresis.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Issue #924 Visibility**: Add "Safe Mode" and "GNSS Throttled" (A15 Hysteresis) status indicators to the HUD/Dashboard UI to explain reduced telemetry rates to the user.
*   **Energy Audit Integration**: Map `ForensicAuditor` energy footprint verdicts (R-ID 259) to `DiagnosticState` for UI visibility in the forensic log.

## 🟢 Recently Resolved Issues (Sep.08.12)
*   **Issue #936 RESOLVED: Forensic Auditor Consolidation (Idea #3)**. Extracted shared audit logic (Reliability/Jitter) from `TrackerService` and `ViewerService` into a unified `ForensicAuditor` (R-ID 280).
*   **Issue #910 HARDENED: Hydration Watchdog Active Recovery**. Implemented forced re-hydration path for stalls at Level 2 (R-ID 281).

## 🟢 Recently Resolved Issues (Sep.08.10)
*   **Issue #935 RESOLVED: Monotonic Signaling Hardening**. Added monotonic `rt` field to `RealtimeStatus` Protobuf and `TelemetryProtobufMapper` (R-ID 279).

## 🟢 Recently Resolved Issues (Sep.08.00)
*   **Issue #975 RESOLVED: Race Condition Audit**. Implemented reference-counted lifecycle in `HardwareProvider` (R-ID 975/R975b).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 296 (Rules: 53, IDs: 243), Resolved: 945, Open: 2, Testing: 95% (Sub-items: 48), Ideas: 4, QA: 266]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.08.12)*
