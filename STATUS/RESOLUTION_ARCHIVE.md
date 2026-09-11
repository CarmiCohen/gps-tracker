# Hardening Resolution Archive (Sep.11.52)

## 🟢 Resolved in Sep.11.52
*   **Recurring GNSS Jitter on A15 (#945)**:
    *   **Remediation**: Elevated `GNSSThread` priority to `THREAD_PRIORITY_URGENT_DISPLAY` in `HardwareProvider`. Budget hardware (A15) cores were causing scheduling starvation for background threads during sensor-heavy logic pulses, leading to ~9000ms jitter. Priority alignment ensures GNSS status callbacks are processed within the urgent scheduling window (R-ID 260).

## 🟢 Resolved in Sep.11.48
*   **Persistent Reactive Flow Stalls (#946)**:
    *   **Remediation**: Standardized the "vitality pulse" pattern by injecting `systemPulseRt` into all segmented dashboard and HUD flows in `MainViewModel`. Updated `UiStateAggregator` and `DashboardStateProvider` to include this pulse in the output states, bypassing `distinctUntilChanged` stalls during power-state transitions (R-ID 289).

## 🟢 Resolved in Sep.11.46
*   **Invalid "Last Seen" Display (#947)**:
    *   **Remediation**: Synchronized the UI telemetry layer to use `systemPulseRt` (monotonic) for all real-time deltas. Updated `TrackerScreen` and `ViewerScreen` to pass the correct time-base to `TrackerDashboard`/`ViewerDashboard`, resolving the 56-year Epoch offset in the "Last Seen" field.

## 🟢 Resolved in Sep.11.43
*   **Telemetry Backfill Convergence (#923)**:
    *   **Remediation**: Synchronized `HistoryManager.fillRealGap` with forensic audit counters (`backfillAuditCount` and `hourlyBackfillTotal`). This ensures that telemetry points backfilled during sustained offline periods (>10s) are correctly reported in continuity audits, matching the behavior of analytical backfilling.

---
*For older records, see historical git logs. (vSep.11.52)*
