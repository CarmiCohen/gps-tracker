# Project Issues & Hardening Tracking (Sep.11.44)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence in the Sep.11.44 release candidate.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Recurring GNSS Jitter on A15 (#945)**: Event Log reports "GNSS Jitter: 8997ms (Hardware Instability)" despite `GNSSThread` implementation. Indicates persistent scheduling contention or driver-level latency on SM-A155F.
*   **Persistent Reactive Flow Stalls (#946)**: Event Log reports "Reactive flow stall detected (Power)" during session start. Vitality monitoring still triggering on power-related state changes.
*   **Invalid "Last Seen" Display (#947)**: Dashboard reports "29,786,995m" (Epoch 1970 delta) due to monotonic vs wall-clock time-base mismatch in `DashboardStateProvider`.

## 🟢 Recently Resolved Issues (Sep.11.44)
*   *(None)*

## 🟢 Recently Resolved Issues (Sep.11.43)
*   **Telemetry Backfill Convergence RESOLVED (#923)**:
    *   **Root-Cause Remediation**: Synchronized `HistoryManager.fillRealGap` with forensic audit counters (`backfillAuditCount` and `hourlyBackfillTotal`). This ensures that telemetry points backfilled during sustained offline periods (>10s) are correctly reported in continuity audits, matching the behavior of analytical backfilling.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 997, Open: 3, Testing: 100% (Sub-items: 51), Ideas: 10, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.44)*
