# Project Issues & Hardening Tracking (Sep.11.48)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Recurring GNSS Jitter on A15 (#945)**: Event Log reports "GNSS Jitter: 8997ms (Hardware Instability)" despite `GNSSThread` implementation. Indicates persistent scheduling contention or driver-level latency on SM-A155F.

## 🟢 Recently Resolved Issues (Sep.11.48)
*   **Persistent Reactive Flow Stalls (#946)**:
    *   **Root-Cause Remediation**: Standardized the "vitality pulse" pattern by injecting `systemPulseRt` into all segmented dashboard and HUD flows in `MainViewModel`. Updated `UiStateAggregator` and `DashboardStateProvider` to include this pulse in the output states, bypassing `distinctUntilChanged` stalls during power-state transitions (R-ID 289).

## 🟢 Recently Resolved Issues (Sep.11.46)
*   **Invalid "Last Seen" Display (#947)**:
    *   **Root-Cause Remediation**: Synchronized the UI telemetry layer to use `systemPulseRt` (monotonic) for all real-time deltas. Updated `TrackerScreen` and `ViewerScreen` to pass the correct time-base to `TrackerDashboard`/`ViewerDashboard`, resolving the 56-year Epoch offset in the "Last Seen" field.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 259 (Rules: 58, IDs: 259), Resolved: 999, Open: 1, Testing: 100% (Sub-items: 51), Ideas: 10, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.48)*
