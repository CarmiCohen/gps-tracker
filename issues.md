# Project Issues & Hardening Tracking (Sep.07.82)

## 🎯 Current Resumption Focus: Forensic Auditor Consolidation
Extract shared audit logic into a unified ForensicAuditor (Simplicity Idea #3).

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)
*   **Idea #3: Forensic Auditor Consolidation**. Shared audit logic between Tracker and Viewer services should be unified to reduce redundancy.

## 🟢 Recently Resolved Issues (Sep.07.82)
*   **Issue #975 HARDENED: HUD Ghosting Remediation**. Remediated the "green TRK LED" bug when switching modes on the same device. Fixed by explicitly clearing the `TelemetryRepository` and `RemoteStatusRepository` singleton states in `SessionUseCase` and `ConnectivitySuite` during mode transitions, preventing stale activity timestamps from being misinterpreted by the new role.

## 🟢 Recently Resolved Issues (Sep.07.70)
*   **Issue #975 RESOLVED: Service Mutual Exclusivity**. Enforced termination of the opposite role service during mode transitions in `MainActivity` to prevent "ghost" telemetry and HUD false-positives during single-device testing.

## 🟢 Recently Resolved Issues (Sep.07.61)
*   **HUD LED Specification Compliance (R960/R972)**: Remediated false-positive green indicators for `VWR` and `DAT` in Tracker Mode. Gated `DAT` strictly to Viewer Mode and restricted `VWR` activity resets to genuine peer pulses, preventing generic signaling heartbeats from masking peer absence.

## 🟢 Recently Resolved Issues (Sep.07.60)
*   **Issue #935 RESOLVED: GPS Red-Lock Regression**. Remediated critical HUD signaling latency where the GPS badge remained RED despite active GNSS callbacks. Fixed by correctly populating the monotonic `rt` field in local `LocationUpdate` emissions from `TrackerService` and `ViewerService`.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 291 (Rules: 52, IDs: 239), Resolved: 941, Open: 0, Testing: 95% (Sub-items: 48), Ideas: 5, QA: 266]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.07.82)*
