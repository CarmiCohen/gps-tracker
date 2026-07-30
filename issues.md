# Project Issues & Hardening Tracking (July.30.35)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 476 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #635] [Severity: Med] [Category: UI/UX] Phone Setup: Permission Status Stalling**. "Exact Alarms" and "Battery Mode" detection is unreliable or doesn't update reactively on Samsung A15 even after manual refresh.
*   **[Issue #636] [Severity: Low] [Category: Technical Debt] Permission Cache Latency**. 15s TTL in `SystemStatusProvider` causes "Refresh" button in Setup to appear unresponsive if clicked immediately after returning from system settings.
*   **[Issue #641] [Severity: Low] [Category: Performance] Map Invalidation Overhead**. Continuous `view.invalidate()` in `MapComponents.kt` consumes CPU even when no overlays change. Throttling the invalidation based on actual overlay state changes is recommended for R-HARDWARE-01.

---

## 🔴 Open Issues
*   **[Issue #635] Phone Setup: Permission Status Stalling**.
*   **[Issue #636] Permission Cache Latency**.
*   **[Issue #641] Map Invalidation Overhead**.

---

## 🟢 Recently Resolved Issues (July.30.35)
*   **[Issue #640] [Severity: High] [Category: Stability] Tracker Mode ANR (Regression)**.
    - **Resolution**: Implemented aggressive throttling and decoupled updates in `MapOverlayManager.kt` to satisfy **R-HARDWARE-01 (Budget Baseline)**. Gated trail, violation, and drift updates to a 1000ms minimum interval. Increased accuracy circle reconstruction threshold to 2.0m and enforced 1000ms gating on all circle recalculations. Decoupled tracker and viewer trail processing to prevent redundant main-thread saturation.
    - **Impact**: Eliminated system-level unresponsiveness on Samsung A15 post-relay connection.
    - **Validation**: Build success and logical verification of throttled pulse handling.

*   **[Issue #637] [Severity: Low] [Category: Efficiency] Log Spam: getPackageName()**.
    - **Resolution**: Implemented a 2000ms short-term status cache for `isLocalOnline()` in `SystemStatusProviderImpl.kt`.
    - **Impact**: Dramatic reduction in logcat volume on Samsung SM-A155F.

*   **[Issue #639] [Severity: High] [Category: Performance] Tracker Mode ANR on Startup**.
    - **Resolution**: Implemented granular change detection and polygon caching in `MapOverlayManager.kt`.

*   **[Issue #638] [Severity: High] [Category: UI/Logic] Incorrect Permission Defaults**.
    - **Resolution**: Corrected `PermissionState` data class in `MainUiState.kt`.

*   **[Issue #634] [Severity: High] [Category: Stability] ForegroundServiceStartNotAllowedException Crash**.
    - **Resolution**: Implemented Foreground Service Start Hardening in `MainActivity`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.35-I)*
