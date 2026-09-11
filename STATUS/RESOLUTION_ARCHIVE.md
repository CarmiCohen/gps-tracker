# Hardening Resolution Archive (Sep.11.48)

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
*For older records, see historical git logs. (vSep.11.48)*
