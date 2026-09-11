# Hardening Resolution Archive (Sep.11.46)

## 🟢 Resolved in Sep.11.46
*   **Invalid "Last Seen" Display (#947)**:
    *   **Remediation**: Synchronized the UI telemetry layer to use `systemPulseRt` (monotonic) for all real-time deltas. Updated `TrackerScreen` and `ViewerScreen` to pass the correct time-base to `TrackerDashboard`/`ViewerDashboard`, resolving the 56-year Epoch offset in the "Last Seen" field.

## 🟢 Resolved in Sep.11.43
*   **Telemetry Backfill Convergence (#923)**:
    *   **Remediation**: Synchronized `HistoryManager.fillRealGap` with forensic audit counters (`backfillAuditCount` and `hourlyBackfillTotal`). This ensures that telemetry points backfilled during sustained offline periods (>10s) are correctly reported in continuity audits, matching the behavior of analytical backfilling.

## 🟢 Resolved in Sep.11.42
*   **GNSS Jitter & Stability Gaps (#916)**:
    *   **Remediation**: Eliminated false-positive stability gap reports by passing the dynamic `currentIntervalMs` to the `ForensicAuditor`.
    *   **Hardware Decoupling**: Decoupled GNSS status callbacks into a dedicated `GNSSThread` in `HardwareProvider` to resolve 9000ms jitter caused by scheduling contention.

---
*For older records, see historical git logs. (vSep.11.46)*
