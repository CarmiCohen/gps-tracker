# Project Issues & Hardening Tracking (Sep.06.58)

## 🎯 Current Resumption Focus: Forensic Auditor Consolidation
Extract shared audit logic into a unified ForensicAuditor (Simplicity Idea #3).

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)
*   **Idea #3: Forensic Auditor Consolidation**. Shared audit logic between Tracker and Viewer services should be unified to reduce redundancy.

## 🟢 Recently Resolved Issues (Sep.06.58)
*   **Issue #935 RESOLVED: GPS Red-Lock Regression**. Remediated critical HUD signaling latency where the GPS badge remained RED despite active GNSS callbacks. Fixed by correctly populating the monotonic `rt` field in local `LocationUpdate` emissions from `TrackerService` and `ViewerService`.

## 🟢 Recently Resolved Issues (Sep.06.57)
*   **Issue #936 VERIFIED: Soak Test Initiation & Forensic Audit**. Deployed vSep.06.57. Verified Energy Footprint (R-ID 259) and Sensor Rate (R-ID 256) audits are functional on physical A15 hardware.

## 🟢 Recently Resolved Issues (Sep.06.56)
*   **Issue #934 RESOLVED: Documentation Integrity Restoration**. Restored accidentally truncated forensic requirements (R251-R267) in `STATUS/QA_VALIDATION_STATUS.md` to maintain the high-assurance audit record.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 288 (Rules: 50, IDs: 238), Resolved: 936, Open: 0, Testing: 90% (Sub-items: 46), Ideas: 226, QA: 262]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.58)*
