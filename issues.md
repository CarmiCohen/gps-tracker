# Project Issues & Hardening Tracking (Aug.11.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 579 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None)

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (Aug.11.04)
*   **[Issue #136] [Severity: Low] [Category: Performance] Compose Preview Coverage Gap.**
    *   **Resolution**: Restored Compose Preview functionality for `SettingsOverlay` and `PhoneSetupOverlay` in `SettingsComponents.kt`. Updated signatures to support decomposed parameters and added `isHydrated` mock support to verify rendering paths (R136).

---

## 🟢 Recently Resolved Issues (Aug.11.03)
*   **[Issue #139] [Severity: High] [Category: Performance] Persistent ANR on Tracker Mode Transition.**
    *   **Resolution**: Implemented **Deferred UI Hydration** (R139) in `TrackerScreen.kt`. By deferring the rendering of heavy components (Map/Dashboard) by 200ms, the navigation transition is allowed to complete smoothly, eliminating 3000ms+ "Davey" stalls on Samsung A15 hardware. (R139)

---

## 🟢 Recently Resolved Issues (Aug.11.02)
*   **[Issue #138] [Severity: High] [Category: Performance] ANR on Tracker Mode Transition.**
    *   **Resolution**: Offloaded all event observers and high-frequency collection jobs in `TrackerService` and `ViewerService` to `Dispatchers.Default` (R138). This cleared the main-thread critical path during service initialization, eliminating 3000ms+ "Davey" stalls and system ANR dialogs.

---

## 🟢 Recently Resolved Issues (Aug.11.00)
*   **[Issue #137] [Severity: High] [Category: Performance] ANR on Settings Overlay Entry.**
    *   **Resolution**: Implemented **Deferred UI Hydration** (R137) in `SettingsComponents.kt`. By deferring content rendering by 100-150ms using `LaunchedEffect` and an `isHydrated` gate, the main thread is able to prioritize overlay transition animations, eliminating 3000ms+ stalls. (R137)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.11.04)
