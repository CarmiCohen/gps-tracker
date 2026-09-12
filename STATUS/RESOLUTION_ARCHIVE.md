# Hardening Resolution Archive (Sep.12.12)

## 🟢 Resolved in Sep.12.12
*   **Rapid Display Flickering (#1010)**:
    *   **Remediation**: Refined `HardwareProvider.displayListener` to ignore volatility between `STATE_DOZE` and `STATE_DOZE_SUSPEND`. These transitions are typical for Samsung AOD (S21FE) during background hydration and do not represent UI performance degradation. Hardened the flickering detector to maintain integrity for active state transitions while suppressing low-power noise. (R-ID 290).

## 🟢 Resolved in Sep.12.02
*   **HUD Mapping Centralization (#1008)**:
    *   **Remediation**: Unified HUD and Dashboard state construction logic into a single stateless `UiStateMapper`. Decommissioned `UiStateAggregator` and `DashboardStateProvider` to prevent arity drift and simplify `MainViewModel` flow combinations. This ensures a single source of truth for UI state mapping and reduces boilerplate (R-ID 286).

## 🟢 Resolved in Sep.12.00
*   **Hardware Flag Abstraction (#1007)**:
    *   **Remediation**: Consolidated hardware LED bitmask flags into a type-safe `LedStatus` data class. Refactored `TrackerService` and `ViewerService` to use `JdHardwareManager.syncHardwareState(..., LedStatus)`, eliminating manual bitwise operations and improving arity safety. This ensures that future LED additions do not require manual bitmask arithmetic in the service layer (R-ID 264).

## 🟢 Resolved in Sep.11.60
*   **GNSS Stability Muzzling Centralization (#1006)**:
    *   **Remediation**: Centralized polling adaptation muzzling into `ForensicAuditor` and `LocationProcessor`. Components now track interval history internally to automatically suppress false-positive stability gaps and jumps during transitions. (R-ID 262).
*   **HUD LED Specification Compliance (#917)**:
    *   **Remediation**: Implemented full hardware LED synchronization for A15 devices as per HUD Spec R338/R972. Updated `JdHardwareManager.syncState` to propagate GPS staleness, internet loss, and relay connectivity status. (R-ID 263).

---
*For older records, see historical git logs. (vSep.12.12)*
