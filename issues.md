# Project Issues & Hardening Tracking (Sep.12.02)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and identity adoption have been fully remediated. Focus shifts to final verification of hardware LEDs and telemetry backfill convergence.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority engine issues currently open.*

## 🟢 Recently Resolved Issues (Sep.12.02)
*   **HUD Mapping Centralization (#1008)**:
    *   **Root-Cause Remediation**: Unified HUD and Dashboard state construction logic into a single stateless `UiStateMapper`. Decommissioned `UiStateAggregator` and `DashboardStateProvider` to prevent arity drift and simplify `MainViewModel` flow combinations. (R-ID 286).

## 🟢 Recently Resolved Issues (Sep.12.00)
*   **Hardware Flag Abstraction (#1007)**:
    *   **Root-Cause Remediation**: Consolidated hardware LED bitmask flags into a type-safe `LedStatus` data class. Refactored `TrackerService` and `ViewerService` to use `JdHardwareManager.syncHardwareState(..., LedStatus)`, eliminating manual bitwise operations and improving arity safety (R-ID 264).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 286 (Rules: 62, IDs: 286), Resolved: 1008, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 14, QA: 271]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.12.02)*
