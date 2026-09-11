# Hardening Resolution Archive (Sep.11.43)

## 🟢 Resolved in Sep.11.43
*   **Telemetry Backfill Convergence (#923)**:
    *   **Remediation**: Synchronized `HistoryManager.fillRealGap` with forensic audit counters (`backfillAuditCount` and `hourlyBackfillTotal`). This ensures that telemetry points backfilled during sustained offline periods (>10s) are correctly reported in continuity audits, matching the behavior of analytical backfilling.

## 🟢 Resolved in Sep.11.42
*   **GNSS Jitter & Stability Gaps (#916)**:
    *   **Remediation**: Eliminated false-positive stability gap reports by passing the dynamic `currentIntervalMs` to the `ForensicAuditor`.
    *   **Hardware Decoupling**: Decoupled GNSS status callbacks into a dedicated `GNSSThread` in `HardwareProvider` to resolve 9000ms jitter caused by scheduling contention with high-frequency sensor processing on budget hardware (A15).
    *   **Vitality Pulses**: Hardened flow vitality by ensuring heartbeat emissions even during polling relaxation.

## 🟢 Resolved in Sep.11.41
*   **Reactive Flow Stalls (#915)**: Decoupled vitality monitoring from state-change detection by removing `.distinctUntilChanged()` from low-level shared flows.

---
*For older records, see historical git logs. (vSep.11.43)*
