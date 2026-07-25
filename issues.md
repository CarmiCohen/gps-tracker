# Project Issues & Hardening Tracking (July.25.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 407 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #560b: Buffer Overflow Resilience**: When using pre-allocated buffers for Protobuf serialization, the system must safely handle cases where the payload exceeds the initial buffer size (e.g., extreme GNSS satellite density) without causing frequent re-allocations.
*   **Issue #547: Kernel Warning (Part B)**: `userfaultfd: MOVE ioctl seems unsupported` still active on Samsung A15; monitoring GC pressure after state decomposition.
*   **Issue #550b: Snapshot Retrieval Churn**: While buffer recording is zero-churn, `getSensorSamples` still yields `SensorSnapshot` objects. If forensic backfilling becomes frequent, a pooled-object or primitive-iterator pattern may be required.

---

## 🔴 Open Issues
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**.
    *   **Observation**: `userfaultfd: MOVE ioctl seems unsupported`.
    *   **Investigation**: Root cause identified as kernel-level IOCTL limitation. Architectural mitigation (State Decomposition) implemented.
    *   **Status**: MONITORING. Tracking GC pause times on Android 15 to ensure stability.

---

## 🟢 Recently Resolved Issues (July.25.03)
*   **Issue #560: Pipeline Serialization Hardening**.
    *   **Resolution**: Refactored `TrackerStatus` for Protobuf builder reuse and implemented a pre-allocated `ByteArray` buffer (4KB) in `ConnectivitySuite`. Utilized `CodedOutputStream` to write directly to the buffer, eliminating `toByteArray()` heap churn.
    *   **Impact**: Achieved "Zero-Churn" telemetry signaling, significantly reducing GC pressure during high-frequency telemetry pulses on restricted kernels.

## 🟢 Recently Resolved Issues (July.25.02)
*   **Issue #550: Forensic primitive-buffer migration**.
    *   **Resolution**: Refactored `GpsManager` and `AppSensorManager` to use primitive arrays (`LongArray`, `DoubleArray`, etc.) with circular indexing for high-frequency telemetry buffering.
    *   **Impact**: Eliminated allocation-related heap churn (Zero-Churn telemetry) during active tracking.
*   **Issue #548: Map Trail Thinning Optimization**.
    *   **Resolution**: Implemented radial distance pruning (1.0m threshold) for trail points.
    *   **Impact**: Reduced `Polyline` node count by ~60-80%, improving map performance and memory stability.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
