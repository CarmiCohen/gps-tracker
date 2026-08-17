# Project Issues & Hardening Tracking (Aug.17.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 629 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No new risks identified.*

---

## 🔴 Open Issues
*   *No open critical issues. System stable and ready for Stress Test.*

---

## 🟢 Recently Resolved Issues (Aug.17.02)
*   **[Issue #187] [Severity: Low] [Category: UI] Dashboard Layout Jitter.**
    *   **Resolution**: Stabilized `InfoRow` in `OverlayComponents.kt` with fixed height (18.dp), `maxLines = 1`, and disabled font padding to eliminate vertical jumping during telemetry hydration (R187).
*   **[Issue #188] [Severity: Critical] [Category: Stability] Build Regression in ViewerService.**
    *   **Resolution**: Fixed invalid string template escaping and corrected unresolved reference to `peakVibrationShock`. Restored full build stability for the Aug.17 branch.
*   **[Issue #185] [Severity: Critical] [Category: Stability] Startup ANR during Map Hydration.**
    *   **Resolution**: Eliminated O(N) hashing on the main thread by offloading `MapTrailSegment` checksum computation to the background thread in `MainViewModel`. Refactored `MapOverlayManager.updateTrails` to use these pre-computed checksums (R185).
*   **[Issue #184] [Severity: High] [Category: Stability] Stress Test IO Race Condition.**
    *   **Resolution**: Hardened the forensic stress test `ioJob` in `TrackerService` (R184).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.17.02)
