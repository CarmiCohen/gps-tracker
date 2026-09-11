# Project Issues & Hardening Tracking (Sep.11.52)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority hardening tasks open.*

## 🟢 Recently Resolved Issues (Sep.11.52)
*   **Recurring GNSS Jitter on A15 (#945)**:
    *   **Root-Cause Remediation**: Elevated `GNSSThread` priority to `THREAD_PRIORITY_URGENT_DISPLAY` in `HardwareProvider`. Budget hardware (A15) cores were causing scheduling starvation for background threads during sensor-heavy logic pulses, leading to ~9000ms jitter. Priority alignment ensures GNSS status callbacks are processed within the urgent scheduling window (R-ID 260).

## 🟢 Recently Resolved Issues (Sep.11.48)
*   **Persistent Reactive Flow Stalls (#946)**:
    *   **Root-Cause Remediation**: Standardized the "vitality pulse" pattern by injecting `systemPulseRt` into all segmented dashboard and HUD flows in `MainViewModel`. Updated `UiStateAggregator` and `DashboardStateProvider` to include this pulse in the output states, bypassing `distinctUntilChanged` stalls during power-state transitions (R-ID 259).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 260 (Rules: 59, IDs: 260), Resolved: 1000, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 12, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.52)*
