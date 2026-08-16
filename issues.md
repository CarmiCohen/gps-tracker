# Project Issues & Hardening Tracking (Aug.16.13)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 627 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #186] [Severity: Medium] [Category: Performance] Forensic Test UI Responsiveness.**
    *   **Risk**: While the ANR is resolved, the UI may still feel sluggish during the 100Hz saturation test on API 35 emulators.
    *   **Concern**: We may need to further throttle map invalidation during peak stress.

---

## 🔴 Open Issues
*   *No open critical issues.*

---

## 🟢 Recently Resolved Issues (Aug.16.13)
*   **[Issue #185] [Severity: Critical] [Category: Stability] Startup ANR during Map Hydration.**
    *   **Resolution**: Eliminated O(N) hashing on the main thread by offloading `MapTrailSegment` checksum computation to the background thread in `MainViewModel`. Refactored `MapOverlayManager.updateTrails` to use these pre-computed checksums, freeing the UI budget during hydration of the initial 2,000 points (R185).
*   **[Issue #184] [Severity: High] [Category: Stability] Stress Test IO Race Condition.**
    *   **Resolution**: Hardened the forensic stress test `ioJob` in `TrackerService` with unique timestamps in filenames and internal try-catch blocks to prevent `FileNotFoundException` (R184).
*   **[Issue #183] [Severity: Critical] [Category: Performance] Startup OOM in Tracker Mode.**
    *   **Resolution**: Reduced trail and violation retrieval limits from 10,000 to 2,000 in `Database.kt` (R183).
*   **[Issue #182] [Severity: Critical] [Category: Environment] Startup ANR & GC Thrashing.**
    *   **Resolution**: Optimized `MapOverlayManager` to reuse cached `GeoPoint` objects. Increased `STARTUP_SETTLING_DELAY_MS` to 10s (R182).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.16.13)
