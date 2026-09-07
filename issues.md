# Project Issues & Hardening Tracking (Sep.06.57)

## 🎯 Current Resumption Focus: GPS Lock Latency Remediation
Investigating post-hydration signaling delays on Samsung A15.

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)
*   **Issue #935: GPS Indicator Latency/Red-Lock (Post-Hydration)**. During soak testing on Samsung A15 (vSep.06.57), the `GPS` HUD badge remains RED despite GNSS callback registration. Preliminary logs suggest a race condition during `ManagedLocationCallback` re-registration or a stale-check threshold issue in `UiStateAggregator` (R-ID 276).

## 🟢 Recently Resolved Issues (Sep.06.57)
*   **Issue #936 VERIFIED: Soak Test Initiation & Forensic Audit**. Deployed vSep.06.57. Verified Energy Footprint (R-ID 259) and Sensor Rate (R-ID 256) audits are functional on physical A15 hardware.

## 🟢 Recently Resolved Issues (Sep.06.56)
*   **Issue #934 RESOLVED: Documentation Integrity Restoration**. Restored accidentally truncated forensic requirements (R251-R267) in `STATUS/QA_VALIDATION_STATUS.md` to maintain the high-assurance audit record.

## 🟢 Recently Resolved Issues (Sep.06.55)
*   **Issue #933 RESOLVED: Viewer Forensic Parity (Audit & Revival)**. Implemented the `Stability Audit` loop and `Revival Event` observation in `ViewerService`.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 288 (Rules: 50, IDs: 238), Resolved: 935, Open: 1, Testing: 90% (Sub-items: 46), Ideas: 225, QA: 262]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.57)*
