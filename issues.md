# Project Issues & Hardening Tracking (Sep.11.10)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority open issues identified.*

## 🟢 Recently Resolved Issues (Sep.11.10)
*   **Map Partitioning Integrity RESOLVED (#243-Audit-Initial)**:
    *   **Root-Cause Remediation**: Eliminated "leftovers of leftovers" by removing redundant map tool overlays and individual parameters from `TrackerScreen.kt` and `ViewerScreen.kt`. Optimized `MainViewModel.kt` with trigger pruning via `MapUiParts` to ensure map state updates only on relevant changes (R-ID 287).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 312 (Rules: 58, IDs: 254), Resolved: 985, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 7, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.10)*
