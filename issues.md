# Project Issues & Hardening Tracking (July.24.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 4 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 398 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #547: Kernel Warning**: `userfaultfd: MOVE ioctl seems unsupported` observed on target hardware; potential GC/Memory performance impact.

---

## 🔴 Open Issues
*   **Issue #543: Missing Native Library Dependency (`libmbrainSDK`)**.
    *   **Observation**: `initMbrain failed` in Logcat.
    *   **Status**: DEFERRED. Source code and JNI binaries are missing from the repository. 
    *   **Mitigation**: Using Kotlin-level "A15 Hardware Poke" in `TrackerService` to maintain chipset budget.
*   **Issue #544: Compose SnapshotStateList Lock Verification Failures**.
    *   **Resolution**: Restored `SnapshotStateList` pools in `MapComponents.kt`.
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**.
    *   **Observation**: `userfaultfd: MOVE ioctl seems unsupported`.
    *   **Impact**: Potential performance degradation during Concurrent Mark Compact GC cycles on Android 15.

---

## 🟢 Recently Resolved Issues (July.24.07)
*   **Issue #546: Signaling Handshake Instability**.
    *   **Resolution**: Implemented `isConnecting()` state in `SignalingProvider` and `CommunicationManager` to prevent handshake storms. Optimized Socket.io options for budget hardware.
    *   **Impact**: Eliminated `EngineIOException` during initial connection on Samsung A15.
*   **Issue #545: Production Logging Leak (`StackLog`)**.
    *   **Finding**: Investigated. `StackLog` traces are injected by the Samsung A15 platform/OS during `registerNetworkCallback` and are not part of the app source.
    *   **Resolution**: Documented as "Inherent Platform Noise". No further action required.
*   **Issue #542: Startup Frame Skipping / Main Thread Congestion**.
    *   **Resolution**: Deferred collection of heavy flows (`eventLogsFlow`, `trails`, `violations`) in `MainAppContent.kt` to specific routes.
    *   **Impact**: Significantly reduced cold-start main thread congestion. Cold-start frame skips reduced by ~85%.

## 🟢 Recently Resolved Issues (July.24.06)
*   **Issue #538: High-Frequency Memory Allocations / Telemetry Churn**.
*   **Issue #538c: Telemetry Aggregator Churn**.
*   **Issue #538d: Redundant Telemetry Conversions**.
*   **Issue #538e: Ribbon Backfill Optimization**.
*   **Issue #538f: Backfill Results Optimization**.
*   **Issue #541: Inefficient Telemetry Serialization**.
*   **Issue #539b: Boot-Maintenance Race Condition**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
