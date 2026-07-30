# Project Issues & Hardening Tracking (July.30.31)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 474 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #640] [Severity: High] [Category: Stability] Tracker Mode ANR (Regression/New)**. App froze on Map screen on Samsung A15 immediately after relay connection. Requires investigation into Main-thread contention during coordinate projection or marker injection.
*   **[Issue #635] [Severity: Med] [Category: UI/UX] Phone Setup: Permission Status Stalling**. "Exact Alarms" and "Battery Mode" detection is unreliable or doesn't update reactively on Samsung A15 even after manual refresh.
*   **[Issue #636] [Severity: Low] [Category: Technical Debt] Permission Cache Latency**. 15s TTL in `SystemStatusProvider` causes "Refresh" button in Setup to appear unresponsive if clicked immediately after returning from system settings.

---

## 🔴 Open Issues
*   **[Issue #640] Tracker Mode ANR (Regression/New)**.
*   **[Issue #635] Phone Setup: Permission Status Stalling**.
*   **[Issue #636] Permission Cache Latency**.

---

## 🟢 Recently Resolved Issues (July.30.31)
*   **[Issue #637] [Severity: Low] [Category: Efficiency] Log Spam: getPackageName()**.
    - **Resolution**: Implemented a 2000ms short-term status cache for `isLocalOnline()` in `SystemStatusProviderImpl.kt`. This throttles high-frequency IPC calls to `ConnectivityManager` that were triggering Samsung "Kumiho" auditing log saturation.
    - **Impact**: Dramatic reduction in logcat volume and main-thread overhead on Samsung SM-A155F.
    - **Validation**: Logcat verification confirms cessation of repetitive `getPackageName` entries during logic pulses.

*   **[Issue #639] [Severity: High] [Category: Performance] Tracker Mode ANR on Startup**.
    - **Resolution**: Implemented granular change detection and polygon caching in `MapOverlayManager.kt`. Added a 1.0m movement/drift threshold for accuracy circle reconstructions. Switched to $O(1)$ size-based guards for trail and violation overlay updates to prevent Main-thread blockage during the initial map render pulse.
    - **Impact**: Eliminated system-level unresponsiveness (ANR) when entering Tracker Mode on Samsung A15.
    - **Validation**: Verified through code audit and manual UI transition testing. (Note: A similar ANR re-occurred as Issue #640 post-connection).

*   **[Issue #638] [Severity: High] [Category: UI/Logic] Incorrect Permission Defaults**.
    - **Resolution**: Corrected `PermissionState` data class in `MainUiState.kt` to default critical permissions to `false`.
    - **Impact**: Ensures accurate Phone Setup UI status upon initialization.

*   **[Issue #634] [Severity: High] [Category: Stability] ForegroundServiceStartNotAllowedException Crash**.
    - **Resolution**: Implemented Foreground Service Start Hardening in `MainActivity`.
    - **Impact**: Eliminates startup crashes on Samsung A15 during automatic restoration.

*   **[Issue #632] [Severity: Med] [Category: UI/Forensic] Analytical Ribbons: Recovery Markers**.
    - **Resolution**: Integrated service recovery blackout markers into high-frequency Analytical Ribbons.
    - **Impact**: Provides visual confirmation of forensic service restoration points.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.31-I)*
