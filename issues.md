# Project Issues & Hardening Tracking (Aug.28.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 45 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 750 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.01)
*   **Concern #750: Native Connectivity Leak**. Deployment regression testing confirmed that `BaseEventQueue` disposal warnings persisted due to `NetworkCallback` objects in `ConnectivitySuite` and `SystemStatusProvider` being garbage collected without deterministic unregistration. Hardened both components to perform synchronous unregistration on the Main Looper during shutdown/awaitClose (R750).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.01)
