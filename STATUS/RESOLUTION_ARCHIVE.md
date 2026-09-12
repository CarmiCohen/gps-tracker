# Hardening Resolution Archive (Sep.12.00)

## 🟢 Resolved in Sep.12.00
*   **Hardware Flag Abstraction (#1007)**:
    *   **Remediation**: Consolidated hardware LED bitmask flags into a type-safe `LedStatus` data class. Refactored `TrackerService` and `ViewerService` to use `JdHardwareManager.syncHardwareState(..., LedStatus)`, eliminating manual bitwise operations and improving arity safety. This ensures that future LED additions (like R972 Peer Stale) do not require manual bitmask arithmetic in the service layer (R-ID 264).

## 🟢 Resolved in Sep.11.60
*   **GNSS Stability Muzzling Centralization (#1006)**:
    *   **Remediation**: Centralized polling adaptation muzzling into `ForensicAuditor` and `LocationProcessor`. Components now track interval history internally via `updateExpectedInterval()` to automatically suppress false-positive stability gaps and jumps during transitions. This eliminates redundant local muzzling logic and service-side boilerplate in `TrackerService` and `ViewerService` (R-ID 262).
*   **HUD LED Specification Compliance (#917)**:
    *   **Remediation**: Implemented full hardware LED synchronization for A15 devices as per HUD Spec R338/R972. Updated `JdHardwareManager.syncState` to propagate GPS staleness (35s gate), internet loss, and relay connectivity status. This ensures physical LED parity with the UI status row and remediates Requirement R972 (R-ID 263).

---
*For older records, see historical git logs. (vSep.12.00)*
