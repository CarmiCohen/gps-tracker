# Project Issues & Hardening Tracking (Aug.28.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 45 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 751 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None currently identified.*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.28.02)
*   **Concern #750: Native Connectivity Leak (Managed Abstraction)**. Resolved the persistent `BaseEventQueue` disposal warning by implementing `ManagedNetworkCallback` and `ManagedLocationCallback` abstractions. These utilities encapsulate synchronous unregistration (Main Looper + CountDownLatch) to ensure deterministic native handle disposal across all hardware-bound components, resolving the leak root-cause on Samsung A15 (R750).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.28.02)
