# Project Issues & Hardening Tracking (July.24.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 6 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 396 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #545: Logging Leak**: Unidentified `StackLog` traces are flooding Logcat during network initialization.
*   **Issue #546: Signaling Handshake Jitter**: WebSocket errors detected during initial relay handshake on Samsung A15.
*   **Issue #547: Kernel Warning**: `userfaultfd: MOVE ioctl seems unsupported` observed on target hardware; potential GC/Memory performance impact.

---

## 🔴 Open Issues
*   **Issue #543: Missing Native Library Dependency (`libmbrainSDK`)**.
    *   **Observation**: `initMbrain failed` and library load errors in Logcat.
    *   **Impact**: Loss of vendor-specific hardware optimizations for MediaTek/Samsung.
*   **Issue #544: Compose SnapshotStateList Lock Verification Failures**.
    *   **Observation**: Failed lock verification for `conditionalUpdate`.
    *   **Impact**: Degraded performance in reactive UI state updates.
*   **Issue #545: Production Logging Leak (`StackLog`)**.
    *   **Observation**: Full stack traces prefixed with `StackLog` printed during `registerNetworkCallback`.
    *   **Impact**: Logcat flooding and unnecessary string allocation overhead.
*   **Issue #546: Signaling Handshake Instability**.
    *   **Observation**: `EngineIOException: websocket error` during initial connection.
    *   **Impact**: Delayed telemetry sync and potential heartbeat failure.
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**.
    *   **Observation**: `userfaultfd: MOVE ioctl seems unsupported`.
    *   **Impact**: Potential performance degradation during Concurrent Mark Compact GC cycles on Android 15.

---

## 🟢 Recently Resolved Issues (July.24.07)
*   **Issue #542: Startup Frame Skipping / Main Thread Congestion**.
    *   **Resolution**: Deferred collection of heavy flows (`eventLogsFlow`, `trails`, `violations`) in `MainAppContent.kt` to specific routes.
    *   **Impact**: Significantly reduced cold-start main thread congestion.

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
