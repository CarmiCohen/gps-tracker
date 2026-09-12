# Project Issues & Hardening Tracking (Sep.12.12)

## 🎯 Current Resumption Focus: Dual-Device Deployment & Connectivity Verification (S21FE & A15)
Verifying version Sep.12.12 deployment across both test devices and validating real-time telemetry synchronization.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Deployment & Sync Audit (#1009)**:
    *   **Status**: S21FE verified (Sep.12.12). A15 verification BLOCKED.
    *   **Task**: A15 device (SM-A155F) not detected for deployment. Verify hardware connection and re-attempt deployment of version Sep.12.12.

## 🟢 Recently Resolved Issues (Sep.12.12)
*   **Rapid Display Flickering (#1010)**:
    *   **Root-Cause Remediation**: Refined `HardwareProvider.displayListener` to ignore volatility between `STATE_DOZE` and `STATE_DOZE_SUSPEND`. These transitions are typical for Samsung AOD (S21FE) during background hydration and do not represent UI performance degradation. Hardened the flickering detector to maintain integrity for active state transitions while suppressing low-power noise. (R-ID 290).

## 🟢 Recently Resolved Issues (Sep.12.02)
*   **HUD Mapping Centralization (#1008)**:
    *   **Root-Cause Remediation**: Unified HUD and Dashboard state construction logic into a single stateless `UiStateMapper`. Decommissioned `UiStateAggregator` and `DashboardStateProvider` to prevent arity drift and simplify `MainViewModel` flow combinations. (R-ID 286).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 287 (Rules: 62, IDs: 287), Resolved: 1009, Open: 1, Testing: Testing (Sub-items: 271), Ideas: 14, QA: 271]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.12.12)*
