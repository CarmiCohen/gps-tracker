# Project Issues & Hardening Tracking (Sep.01.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 22 |
| **Validation Tasks** | 🟢 Validated | 216 |
| **Resolved (Total)** | 🟢 Progress | 799 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None)

---

## 🔴 Open Issues
*   *(See Dashboard for total count)*

---

## 🟢 Recently Resolved Issues (Sep.01.04)
*   **Issue #880 VALIDATED: Residual Hydration Davey Remediation (R880)**. Deployment on SM-A155F revealed a 751ms stall during hydration. Resolved by increasing hydration delays (to 600ms) and implementing "High-Granularity Yielding" (every 2 items) in `MapOverlayManager` to ensure frame budget compliance (<700ms). (Sep.01.04).
*   **Issue #879 VALIDATED: Forensic Heap Pollution Audit (R879)**. Confirmed via `ForensicStressAuditTest` (100Hz burst stability). Implemented zero-churn read/write paths and reused internal buffers in `ForensicSpillBuffer` to prevent heap pressure during rapid restarts. (Sep.01.02).
*   **Issue #878 VALIDATED: Low-memory map eviction strategy (R878)**. Confirmed via deployment and logcat. `LRU ShadowCache` and `ComponentCallbacks2` integration successfully managing memory pressure without map instability. (Sep.01.01).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.04)*
*Simplification Ideas: 224*
