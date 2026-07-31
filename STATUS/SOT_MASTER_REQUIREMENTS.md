# System Source of Truth (SoT) - July.30.55 (Zero-Churn Hardening)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Budget Baseline Optimization (R-HARDWARE-01)**: The Tracking Engine and UI MUST be optimized for a "Budget Baseline" (Samsung A15). (Issue #640)
*   **Zero-Churn GPS Hot-Path (R653)**: The high-frequency GPS processing chain (`LocationProcessor` -> `LocationSentinel` -> `PhysicsUtils`) MUST NOT allocate new result objects on every fix. All result containers (e.g., `SentinelResult`, `ProcessedLocation`, `JumpConfidence`) MUST utilize mutable flyweights and be reused across ticks. List-based transformations (filter/map/minOf) are prohibited in the 1Hz/10Hz hot-path; indexed loops over pre-allocated or cached collections MUST be used instead. (Issue #653, July.30.55)
*   **Zero-Churn Interpolation (R653b)**: Coordinate interpolation MUST utilize callback-based signaling rather than returning new List instances to eliminate heap churn during trajectory promotion. (Issue #653, July.30.55)
*   **Startup Transition Authority (R658)**: The Main thread MUST remain silent during activity transitions. (Issue #658, July.30.47)
*   **JNI Initialization Integrity (R659)**: `MbrainHardwareManager` MUST verify integrity before every JNI call. (Issue #659, July.30.47)
*   **Hardware IPC Throttling (R645/646/648/652/654)**: System service calls MUST be throttled to a minimum of 5000ms. (Issue #645, July.30.45)
*   **Atomic IPC Throttling (R650/651)**: System service calls prone to auditing MUST be wrapped in a Mutex and offloaded to Dispatchers.IO. (Issue #650)
*   **Zero-Churn Engine Windows (R547b)**: High-frequency kinematic windows MUST utilize circular primitive buffers. (Issue #547b)
*   **Forensic Snapshot Pooling (R570)**: Retrieval of sensor samples MUST utilize mutable flyweight objects. (Issue #570)
*   **Main-Thread Purity (R526)**: The Application's Main thread MUST NOT be blocked by heavy initialization. (Issue #526)

[... Remaining sections preserved ...]

### 6. Version Authority
*   **Current Release**: July.30.55.
*   **Source of Truth**: app/build.gradle versionName.
