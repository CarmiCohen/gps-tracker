# Project Issues & Hardening Tracking (Sep.12.00)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority engine issues currently open.*

## 🟢 Recently Resolved Issues (Sep.12.00)
*   **Hardware Flag Abstraction (#1007)**:
    *   **Root-Cause Remediation**: Consolidated hardware LED bitmask flags into a type-safe `LedStatus` data class. Refactored `TrackerService` and `ViewerService` to use `JdHardwareManager.syncHardwareState(..., LedStatus)`, eliminating manual bitwise operations and improving arity safety (R-ID 264).

## 🟢 Recently Resolved Issues (Sep.11.60)
*   **GNSS Stability Muzzling Centralization (#1006)**:
    *   **Root-Cause Remediation**: Centralized polling adaptation muzzling into `ForensicAuditor` and `LocationProcessor`. Components now track interval history internally to automatically suppress false-positive stability gaps and jumps during transitions, eliminating redundant boilerplate in `TrackerService` and `ViewerService` (R-ID 262).
*   **HUD LED Specification Compliance (#917)**:
    *   **Root-Cause Remediation**: Implemented full hardware LED synchronization for A15 devices as per HUD Spec R338/R972. Updated `JdHardwareManager.syncState` to propagate GPS staleness (35s gate), internet loss, relay connectivity status, and peer presence. (R-ID 263).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 264 (Rules: 61, IDs: 264), Resolved: 1007, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 14, QA: 271]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.12.00)*
