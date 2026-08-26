# Project Issues & Hardening Tracking (Aug.26.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 CAUTION | 49 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 723 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #320**: **Native Resource Leak**. Logcat identifies `A resource failed to call BaseEventQueue.dispose` during `TrackerService` destruction or native release. Potential memory/handle leak.
*   **Issue #321**: **A15 UI Hydration Stall**. 901ms Davey stall detected in `TrackerScreen` layout on A15 hardware. Staggered hydration successfully offloads core logic, but map composition remains heavy.

---

## 🔴 Open Issues
*   **Issue #320**: Native Resource Leak (`BaseEventQueue`).
*   **Issue #321**: UI Composition Performance Stall on A15.

---

## 🟢 Recently Resolved Issues (Aug.26.00 - Aug.26.01)
*   **Issue #318**: **A15 Startup Frame Drops**. Implemented `LifecycleHydrationManager` to stagger startup sequences and offload hydration from the main thread.
*   **Issue #319**: **Background Monitor Inflation Failure**. Hardened native initialization with exponential backoff retries in `JdHardwareManager` to ensure reliable hardware binding.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.26.01)
