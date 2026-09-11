# Project Issues & Hardening Tracking (Sep.11.46)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence in the Sep.11.46 release candidate.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Recurring GNSS Jitter on A15 (#945)**: Event Log reports "GNSS Jitter: 8997ms (Hardware Instability)" despite `GNSSThread` implementation. Indicates persistent scheduling contention or driver-level latency on SM-A155F.
*   **Persistent Reactive Flow Stalls (#946)**: Event Log reports "Reactive flow stall detected (Power)" during session start. Vitality monitoring still triggering on power-related state changes.

## 🟢 Recently Resolved Issues (Sep.11.46)
*   **Invalid "Last Seen" Display (#947)**:
    *   **Root-Cause Remediation**: Synchronized the UI telemetry layer to use `systemPulseRt` (monotonic) for all real-time deltas. Updated `TrackerScreen` and `ViewerScreen` to pass the correct time-base to `TrackerDashboard`/`ViewerDashboard`, resolving the 56-year Epoch offset in the "Last Seen" field.

## 🟢 Recently Resolved Issues (Sep.11.43)
*   **Telemetry Backfill Convergence RESOLVED (#923)**:
    *   **Root-Cause Remediation**: Synchronized `HistoryManager.fillRealGap` with forensic audit counters (`backfillAuditCount` and `hourlyBackfillTotal`).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 998, Open: 2, Testing: 100% (Sub-items: 51), Ideas: 10, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.46)*
