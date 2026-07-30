# Project Issues & Hardening Tracking (July.30.27)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 467 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (July.30.27)
*   **[Issue #629] [Severity: Med] [Category: Forensic] Deferred Recovery Latency Audit**.
    - **Resolution**: Integrated forensic timestamping for deferred service recovery. `WatchdogReceiver` and `MaintenanceWorker` now record the `recovery_blocked_ts` when a background start is restricted. `MainViewModel` calculates and logs the "Service Blackout Duration" (latency) upon successful restoration in the foreground, adhering to **R623** naming conventions.
    - **Impact**: Provides visibility into the effectiveness of the deferred recovery mechanism on Samsung A15 devices.
    - **Validation**: Verified requirement alignment (**R629**).

*   **[Issue #626] [Severity: High] [Category: Stability] Foreground Service Start Restriction**.
    - **Resolution**: Implemented a "Recovery Pending" state. Background components (`WatchdogReceiver`, `MaintenanceWorker`) now catch `ForegroundServiceStartNotAllowedException` and persist a recovery flag. `MainActivity` reactively triggers service restoration in `onResume` when the app enters the foreground, complying with Android 12+ background policies.
    - **Impact**: Eliminates fatal crashes and ensures tracking continuity on devices with aggressive background restrictions (Samsung A15/Android 14+).

---

## 🟢 Recently Resolved Issues (July.30.26)
*   **[Issue #628] [Severity: Med] [Category: Compatibility] 16KB Page Size Support**.
    - **Resolution**: Added linker flag `-Wl,-z,max-page-size=16384` to `CMakeLists.txt` for `libmbrainSDK.so`. Enabled `jniLibs.useLegacyPackaging = true` in `app/build.gradle`.
    - **Impact**: Ensures compatibility with Android 15 hardware.

*   **[Issue #625] [Severity: Med] [Category: Structural] Structural: Mbrain JNI Reliability Audit**.
    - **Resolution**: Hardened `MbrainHardwareManager` with `executeNativeWithRetry` to handle `JNI_RET_EINTR` (-4).
    - **Impact**: Prevents transient hardware communication failures.

*   **[Issue #627] [Severity: High] [Category: Performance] Startup ANR & Main Thread Blocking**.
    - **Resolution**: Offloaded library loading and `initMbrain` calls to `Dispatchers.IO` in `TrackerService`.
    - **Impact**: Eliminates 4s+ UI stall during cold start.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
