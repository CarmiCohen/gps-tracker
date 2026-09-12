# Hardening Resolution Archive (Sep.11.60)

## 🟢 Resolved in Sep.11.60
*   **GNSS Stability Muzzling Centralization (#1006)**:
    *   **Remediation**: Centralized polling adaptation muzzling into `ForensicAuditor` and `LocationProcessor`. Components now track interval history internally via `updateExpectedInterval()` to automatically suppress false-positive stability gaps and jumps during transitions. This eliminates redundant local muzzling logic and service-side boilerplate in `TrackerService` and `ViewerService` (R-ID 262).
*   **HUD LED Specification Compliance (#917)**:
    *   **Remediation**: Implemented full hardware LED synchronization for A15 devices as per HUD Spec R338/R972. Updated `JdHardwareManager.syncState` to propagate GPS staleness (35s gate), internet loss, and relay connectivity status. This ensures physical LED parity with the UI status row and remediates Requirement R972 (R-ID 263).

## 🟢 Resolved in Sep.11.58
*   **A15 GNSS Instability (#950)**:
    *   **Remediation**: Relaxed GNSS stability thresholds to accommodate Samsung A15 hardware scheduling latency (Jitter: 500ms -> 3000ms, Gap: 200ms -> 1000ms). Implemented transition "muzzling" in `ForensicAuditor` to suppress false-positive stability gaps during polling interval adaptation (e.g., stationary to moving transitions). Propagated `isAdaptationMuzzled` flag from `TrackerService` and `ViewerService` to the auditor (R-ID 262).

## 🟢 Resolved in Sep.11.56
*   **A15 Reactive Flow Stalls (#949)**:
    *   **Remediation**: Directed all shared observation flows in `SystemStatusProviderImpl` to execute on `Dispatchers.IO` using `.flowOn(Dispatchers.IO)`. This eliminates main-thread contention and ensures consistent vitality pulses for IntegrityMonitor heartbeats, preventing false-positive stall detections on budget hardware (R-ID 289).

---
*For older records, see historical git logs. (vSep.11.60)*
