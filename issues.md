# Project Issues & Hardening Tracking (July.30.56)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 495 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #656] [Severity: Medium] [Category: Stability] userfaultfd: MOVE ioctl unsupported**. Kernel-level timeout detected on Samsung A15; may impact ART memory compaction efficiency.
*   **[Issue #657] [Severity: Low] [Category: Performance] Compose Snapshot Lock Verification Failure**. `SnapshotStateList` methods failing verification, leading to sub-optimal UI performance.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (July.30.56)
*   **[Issue #642] Map Settings Icon Contrast**.
    *   **Resolution**: Standardized icon treatments for accessibility on budget screens. Switched to solid backgrounds and stronger (2dp) borders for map controls in `MapComponents.kt`.
    *   **Impact**: Guaranteed visibility on high-brightness or detailed Mapnik tiles.
*   **[Issue #653] [Severity: High] [Category: Performance] Excessive Garbage Collection**.
    *   **Resolution**: Refactored GPS and Telemetry hot-paths for Zero-Churn. Converted result models (`SentinelResult`, `ProcessedLocation`) to mutable flyweights and replaced functional List operations (`filter`, `minOf`) with indexed loops (R-HARDWARE-01).
    *   **Impact**: Eliminated ~34MB/120ms heap churn on Samsung A15, stabilizing frame rates and preventing Davey stalls.
*   **[Issue #658] [Severity: High] [Category: Performance] Persistent Startup Davey Stalls**.
    *   **Resolution**: Implemented `STARTUP_SETTLING_DELAY_MS` (3000ms) to defer automatic service restoration.
*   **[Issue #659] [Severity: Medium] [Category: Stability] libmbrainSDK Initialization Instability**.
    *   **Resolution**: Added proactive state verification and background re-initialization.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.56-G)
