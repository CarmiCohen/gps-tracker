# Project Issues & Hardening Tracking (Aug.30.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 33 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 774 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None identified in this session.

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.30.00)
*   **Concern #767: Lingering BaseEventQueue Leak (Hardening)**. Identified a native resource leak warning (`BaseEventQueue.dispose` failure) in Logcat during service shutdown. Remediated by implementing fallback direct unregistration logic in `ManagedHardware.kt`. This ensures native disposal completes even if the hardware thread is unresponsive (R767).
*   **Concern #766: RTL Layout Inconsistency and Text Truncation**. Resolved in Aug.29.13. Enforced LTR direction in `StatusBar` and fixed "SIGNAL LOSS" truncation (R766).
*   **Concern #762: Acoustic Refinement (R762b)**. Resolved in Aug.29.12. Encapsulated adaptive duty-cycle in SentinelValidator.
*   **Concern #765: Ultra-Long Stationary State UI Refinement**. Resolved in Aug.29.11. Added visual indicators to HUD and Dashboard.
*   **Concern #765: Ultra-Long Stationary State Exposure**. Resolved in Aug.29.10. Centralized detection in HardwareProvider.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.30.00)
