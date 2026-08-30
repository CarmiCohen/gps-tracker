# Project Issues & Hardening Tracking (Aug.30.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 34 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 774 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #775: Persistent BaseEventQueue Leak (Native)**. Despite implementing fallback direct unregistration in `ManagedHardware.kt` (R767), Logcat still reports `A resource failed to call BaseEventQueue.dispose` during service shutdown. This indicates either a leak in an unmanaged hardware component (e.g., a specific sensor not yet wrapped) or a race condition in native disposal.
*   **Concern #776: Hydration Sequence UI Jank (Davey)**. High-density "Davey" warnings (1500ms+) observed during `MainActivity` hydration on SM-A155F. Likely due to synchronous initialization of map layers or UI components.

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.30.00)
*   **Concern #767: Lingering BaseEventQueue Leak (Hardening)**. Identified a native resource leak warning (`BaseEventQueue.dispose` failure) in Logcat during service shutdown. Remediated by implementing fallback direct unregistration logic in `ManagedHardware.kt`. This ensures native disposal completes even if the hardware thread is unresponsive (R767).
*   **Concern #766: RTL Layout Inconsistency and Text Truncation**. Resolved in Aug.29.13. Enforced LTR direction in `StatusBar` and fixed "SIGNAL LOSS" truncation (R766).
*   **Concern #762: Acoustic Refinement (R762b)**. Resolved in Aug.29.12. Encapsulated adaptive duty-cycle in SentinelValidator.
*   **Concern #765: Ultra-Long Stationary State UI Refinement**. Resolved in Aug.29.11. Added visual indicators to HUD and Dashboard.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.30.01)
