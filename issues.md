# Project Issues & Hardening Tracking (July.25.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 412 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #570b: Flyweight Thread Safety**: While forensic sequences use synchronized access to primitive buffers, the flyweight objects themselves are reused. Consumers must process or copy fields immediately before the next `yield`.
*   **Issue #580b: Native Signal Latency**: While `ReentrantLock` prevents collisions, prolonged native execution in `libmbrainSDK` could theoretically delay the high-frequency tick loop. Monitor `punchHardware` execution time.

---

## 🔴 Open Issues
*   *No critical open issues.*

---

## 🟢 Recently Resolved Issues (July.25.08)
*   **Issue #560c: Signaling Pressure Audit**.
    *   **Resolution**: Implemented a Dual-Queue Priority Dispatcher in `CommunicationManager`. High-priority pulses (Pings, Identity) skip application-layer buffering for immediate emission, while Normal-priority bulk data (64KB telemetry Protobufs) are queued with an inter-frame throttle (50ms) to prevent socket buffer saturation.
    *   **Data Integrity**: Synchronized the `is_clock_regression` field across the Protobuf schema, `TrackerStatus` model, and `ConnectivitySuite` pipeline to ensure forensic regression flags are preserved in expanded binary payloads.
    *   **Impact**: Prevents head-of-line blocking during network congestion, ensuring heartbeats remain stable even when large telemetry payloads are being synchronized.

---

## 🟢 Recently Resolved Issues (July.25.07)
*   **Issue #547b: Kernel I/O Optimization (Samsung A15 Resilience)**.
    *   **Resolution**: Refactored high-frequency engine components (`GtoEngine` and `LocationProcessor`) to use primitive circular buffers (`DoubleArray`, `LongArray`) for kinematic windows and accuracy tracking.
    *   **Impact**: Mitigated performance impact of missing `userfaultfd: MOVE` support on Android 15 (Samsung A15).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
