# Project Issues & Hardening Tracking (Aug.16.14)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 628 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #187] [Severity: Low] [Category: UI] Dashboard Layout Jitter.**
    *   **Risk**: Minor UI jumping when telemetry fields transition from "--" to live values during hydration.

---

## 🔴 Open Issues
*   *No open critical issues.*

---

## 🟢 Recently Resolved Issues (Aug.16.14)
*   **[Issue #186] [Severity: High] [Category: Performance] Gated Sensor Startup.**
    *   **Resolution**: Implemented deferred sensor registration in `AppSensorManager` using `SENSOR_SETTLING_DELAY_MS` (2000ms). This prevents high-frequency IPC/Binder saturation during the critical first 2 seconds of role entry, ensuring the UI thread has exclusive priority for Map/Dashboard hydration (R186).
*   **[Issue #185] [Severity: Critical] [Category: Stability] Startup ANR during Map Hydration.**
    *   **Resolution**: Eliminated O(N) hashing on the main thread by offloading `MapTrailSegment` checksum computation to the background thread in `MainViewModel`. Refactored `MapOverlayManager.updateTrails` to use these pre-computed checksums (R185).
*   **[Issue #184] [Severity: High] [Category: Stability] Stress Test IO Race Condition.**
    *   **Resolution**: Hardened the forensic stress test `ioJob` in `TrackerService` (R184).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.16.14)
