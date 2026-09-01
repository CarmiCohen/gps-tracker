# Project Issues & Hardening Tracking (Sep.01.25)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 20 |
| **Validation Tasks** | 🟢 Validated | 219 |
| **Resolved (Total)** | 🟢 Progress | 813 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #893: Lingering BaseEventQueue Disposal Failure**. Deployment validation of vSep.01.24 shows that `A resource failed to call BaseEventQueue.dispose` still occurs even with the 800ms settling window. This suggests a secondary native resource (likely in `ManagedNetworkCallback` or `FusedLocationProvider`) is bypassing the synchronous teardown sequence. (Sep.01.25).
*   **Issue #894: getPackageName Spam Regression**. The remediation for Issue #759 is being bypassed by system service calls in `ConnectivitySuite` and `ManagedHardware`, causing excessive logcat overhead. (Sep.01.25).

---

## 🔴 Open Issues
*   **Issue #893: Native Resource Disposal Leak (BaseEventQueue)**.
*   **Issue #894: logcat Overhead (getPackageName Spam)**.

---

## 🟢 Recently Resolved Issues (Sep.01.24)
*   **Issue #892 RESOLVED: WorkManager Initialization Failure (R892)**. Resolved `IllegalStateException` in `BootReceiver` by implementing manual initialization in `GpsApplication.onCreate()`. (Verified Sep.01.25).
*   **Issue #891 REFINED: Teardown Sequencing & Settling Alignment**. Enforced the mandated 800ms settling window in `HardwareProvider.stop()`. Note: Disposal warnings persist (see #893). (Sep.01.24).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.25)*
*Simplification Ideas: 231*
