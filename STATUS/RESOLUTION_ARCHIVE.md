# Hardening Resolution Archive (Sep.12.15)

## 🟢 Resolved in Sep.12.12
*   **Rapid Display Flickering (#1010)**:
    *   **Remediation**: Refined `HardwareProvider.displayListener` to ignore volatility between `STATE_DOZE` and `STATE_DOZE_SUSPEND`. These transitions are typical for Samsung AOD (S21FE) during background hydration and do not represent UI performance degradation. Hardened the flickering detector to maintain integrity for active state transitions while suppressing low-power noise. (R-ID 290).

## 🟢 Resolved in Sep.12.02
*   **HUD Mapping Centralization (#1008)**:
    *   **Remediation**: Unified HUD and Dashboard state construction logic into a single stateless `UiStateMapper`. Decommissioned `UiStateAggregator` and `DashboardStateProvider` to prevent arity drift and simplify `MainViewModel` flow combinations. (R-ID 286).

## 🟢 Resolved in Sep.12.00
*   **Hardware Flag Abstraction (#1007)**:
    *   **Remediation**: Consolidated hardware LED bitmask flags into a type-safe `LedStatus` data class. Refactored `TrackerService` and `ViewerService` to use `JdHardwareManager.syncHardwareState(..., LedStatus)`, eliminating manual bitwise operations and improving arity safety. (R-ID 264).

---
*For older records, see historical git logs. (vSep.12.15)*
