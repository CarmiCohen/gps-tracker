# Project Issues & Hardening Tracking (July.25.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 409 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #560b: Buffer Overflow Resilience**: When using pre-allocated buffers for Protobuf serialization, the system must safely handle cases where the payload exceeds the initial buffer size (e.g., extreme GNSS satellite density) without causing frequent re-allocations.
*   **Issue #547: Kernel Warning (Part B)**: `userfaultfd: MOVE ioctl seems unsupported` still active on Samsung A15; monitoring GC pressure after state decomposition.
*   **Issue #570b: Flyweight Thread Safety**: While forensic sequences use synchronized access to primitive buffers, the flyweight objects themselves are reused. Consumers must process or copy fields immediately before the next `yield`.
*   **Issue #580b: Native Signal Latency**: While `ReentrantLock` prevents collisions, prolonged native execution in `libmbrainSDK` could theoretically delay the high-frequency tick loop. Monitor `punchHardware` execution time.

---

## 🔴 Open Issues
*   **Issue #547: Kernel Performance Warning (`userfaultfd`)**.
    *   **Observation**: `userfaultfd: MOVE ioctl seems unsupported`.
    *   **Investigation**: Root cause identified as kernel-level IOCTL limitation. Architectural mitigation (State Decomposition) implemented.
    *   **Status**: MONITORING. Tracking GC pause times on Android 15 to ensure stability.

---

## 🟢 Recently Resolved Issues (July.25.05)
*   **Issue #580: Mbrain JNI Hardening**.
    *   **Resolution**: Conducted forensic audit of `libmbrainSDK` bridge. Implemented `ReentrantLock` in `MbrainHardwareManager` to prevent JNI signal collisions during rapid FGS type transitions. Refactored JNI methods with `native` prefix and added robust null-checking for string conversions in `mbrain-jni.cpp`.
    *   **Impact**: Secured hardware stay-alive pokes against race conditions and memory safety violations.
*   **Issue #570: Forensic Snapshot Pooling**.
    *   **Resolution**: Refactored `EngineConnectionPoint`, `EngineSensorSnapshot`, and `EngineSnrSample` into mutable flyweight classes. Optimized `AppSensorManager`, `GpsManager`, and `TelemetryAggregator` to use these flyweights during forensic backfilling and telemetry pulses.
    *   **Impact**: Achieved "Zero-Churn" forensic reconstruction, eliminating transient heap allocations in the high-frequency backfill path.
*   **Issue #550: Forensic primitive-buffer migration**.
    *   **Resolution**: Refactored `GpsManager` and `AppSensorManager` to use primitive arrays (`LongArray`, `DoubleArray`, etc.) with circular indexing.
    *   **Impact**: Eliminated allocation-related heap churn (Zero-Churn telemetry) during active tracking.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
