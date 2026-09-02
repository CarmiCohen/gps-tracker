# Project Issues & Hardening Tracking (Sep.02.68)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Healthy | 0 |
| **Validation Tasks** | 🟢 Validated | 227 |
| **Resolved (Total)** | 🟢 Progress | 853 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   None.

---

## 🟢 Recently Resolved Issues (Sep.02.68)
*   **Issue #245 RESOLVED: "SYS" Badge Deactivation lifecycle**. Added handlers for `ConfirmStopTracking` and `ManualExit` in `MainViewModel` to ensure `isSystemActive` is toggled false upon session termination, maintaining visual parity (R-ID 245).
*   **Idea #243 RESOLVED: UI State Flattening for StatusBar**. Refactored `StatusBar` and `GlobalStatusBar` to consume the unified `HudState` object. This removed over 40 individual parameters from the signature, improving code maintainability and JIT compilation efficiency on Samsung A15 hardware (R243).
*   **Issue #243 RESOLVED: GlobalStatusBar isSystemActive Pass-through**. Propagated `isSystemActive` flag from `GlobalStatusBar` to `StatusBar` via `HudConnectivityState` and implemented a "SYS" status badge for visual parity (R-ID 243).
*   **Issue #241 RESOLVED: Missing Mode-Selection Activation**. Migrated `setAppMode` to suspend and integrated atomic `IS_SYSTEM_ACTIVE_KEY` toggle in `SessionUseCase` (R-ID 241).
*   **Issue #242 RESOLVED: Unhandled TriggerRecovery Event**. Implemented reactive signal-response pattern for automated foreground service retry (R-ID 242).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.68)*
*Simplification Ideas: 242 Active*
