# Project Issues & Hardening Tracking (Sep.08.10)

## 🎯 Current Resumption Focus: Forensic Auditor Consolidation
Extract shared audit logic into a unified ForensicAuditor (Simplicity Idea #3).

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Idea #3: Forensic Auditor Consolidation**. Move `Stability Audit` (Reliability/Jitter) loops from `TrackerService` and `ViewerService` into `ForensicAuditor` to restore SRP and simplify role-specific services.
*   **Issue #910 Hardening**: Implement active recovery for `Hydration Watchdog` stuck at Level 2 (e.g., triggering a service re-init or forced re-hydration).
*   **Issue #924 Visibility**: Add "Safe Mode" and "GNSS Throttled" (A15 Hysteresis) status indicators to the HUD/Dashboard UI to explain reduced telemetry rates to the user.
*   **Energy Audit Integration**: Map `ForensicAuditor` energy footprint verdicts (R-ID 259) to `DiagnosticState` for UI visibility in the forensic log.

## 🟢 Recently Resolved Issues (Sep.08.10)
*   **Issue #935 RESOLVED: Monotonic Signaling Hardening**. Added monotonic `rt` field to `RealtimeStatus` Protobuf and `TelemetryProtobufMapper`. Eliminates heuristic drift in remote signaling to resolve HUD "Red-Lock" false positives (R-ID 279).

## 🟢 Recently Resolved Issues (Sep.08.00)
*   **Issue #975 RESOLVED: Race Condition Audit**. Implemented reference-counted lifecycle in `HardwareProvider` to ensure deterministic teardown during rapid Tracker/Viewer mode switching (R-ID 975/R975b).

## 🟢 Recently Resolved Issues (Sep.07.82)
*   **Issue #975 HARDENED: HUD Ghosting Remediation**. Remediated the "green TRK LED" bug when switching modes on the same device. Fixed by explicitly clearing repositories in `SessionUseCase` and `ConnectivitySuite`.

## 🟢 Recently Resolved Issues (Sep.07.60)
*   **Issue #935 RESOLVED: GPS Red-Lock Regression**. Remediated critical HUD signaling latency by correctly populating the monotonic `rt` field in local `LocationUpdate` emissions.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 294 (Rules: 53, IDs: 241), Resolved: 943, Open: 4, Testing: 95% (Sub-items: 48), Ideas: 5, QA: 266]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.08.10)*
