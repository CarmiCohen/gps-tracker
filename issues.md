# Project Issues & Hardening Tracking (Sep.12.20)

## 🎯 Current Resumption Focus: Dual-Device Deployment & Connectivity Verification (S21FE & A15)
Verifying version Sep.12.20 deployment across both test devices and validating real-time telemetry synchronization.

## 🔴 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Deployment & Sync Audit (#1009)**:
    *   **Status**: S21FE verified (Sep.12.12). A15 detected via Logcat (Sep.12.15).
    *   **Task**: Re-attempt deployment of version Sep.12.20 to A15 and verify Tracker role integrity.

## 🟢 Recently Resolved Issues (Sep.12.20)
*   **Main-Thread Task Await Regression (#1011)**:
    *   **Root-Cause Remediation**: Remediated by adding a Main-thread check in `ManagedLocationCallback.unregister` to skip `Tasks.await` if called on the Main thread. This prevents `IllegalStateException` during fallback unregistration while maintaining synchronous behavior on background handler threads. (R-ID 291).
*   **Rapid Display Flickering (#1010)**:
    *   **Root-Cause Remediation**: Refined `HardwareProvider.displayListener` to ignore volatility between `STATE_DOZE` and `STATE_DOZE_SUSPEND`. These transitions are typical for Samsung AOD (S21FE) during background hydration and do not represent UI performance degradation. Hardened the flickering detector to maintain integrity for active state transitions while suppressing low-power noise. (R-ID 290).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 288 (Rules: 62, IDs: 288), Resolved: 1011, Open: 1, Testing: 1 (Sub-items: 271), Ideas: 17, QA: 271]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.12.20)*
