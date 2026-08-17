# System Source of Truth (SoT) - Aug.17.01 (Stability Restored)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Viewer Service Stabilization (R188)**: (Added Aug.17.00) The system MUST maintain strict syntax integrity in `ViewerService.kt`. Log messages MUST use standard string formatting, and forensic telemetry MUST correctly reference `peakVibrationShock` from `TrackerStatus` to ensure build stability. (Issue #188). **Status: Implemented.**
*   **Map Hydration Hardening (R185)**: (Added Aug.16.13) The system MUST offload trail segment hashing and simplification to background threads. `MapTrailSegment` MUST contain a pre-computed `checksum` generated in the `MainViewModel`. `MapOverlayManager` MUST utilize these pre-computed checksums for O(1) change detection. (Issue #185). **Status: Implemented.**
*   **Forensic IO Hardening (R184)**: (Added Aug.16.12) Forensic stress test `ioJob` MUST use unique filenames (`forensic_stress_${System.currentTimeMillis()}.bin`) and internal error suppression to prevent service termination during high-frequency disk contention. (Issue #184). **Status: Implemented.**
*   **Startup Hydration Limits (R183)**: (Added Aug.16.10) Trail and violation retrieval limits MUST be capped at 2,000 points to prevent OOM and Binder saturation on resource-constrained devices. (Issue #183). **Status: Implemented.**
*   **Map Allocation Hardening (R182)**: (Added Aug.16.00) The system MUST eliminate allocation churn during map rendering by reusing cached `GeoPoint` objects. `STARTUP_SETTLING_DELAY_MS` is increased to 10000ms. (Issue #182). **Status: Implemented.**
*   **System Startup Hardening (R181)**: (Updated Aug.16.00) The system MUST defer 100Hz telemetry engine start and heavy osmdroid initialization until after a 10000ms settling period. (Issue #181). **Status: Implemented.**
*   **Log Identity Integrity (R180)**: (Updated Aug.15.03) The system MUST utilize `localId` as the primary unique constraint for all log entries. (Issue #180). **Status: Implemented.**
*   **High-Frequency Allocation Hardening (R179)**: (Added Aug.15.01) Throttled UI History Emitter (2Hz) MUST be used to decouple 100Hz telemetry from UI Flow emissions. (Issue #179). **Status: Implemented.**
*   **Forensic Flow Gating (R178)**: (Added Aug.15.01) Heavy data mapping MUST be gated by UI visibility. (Issue #178). **Status: Implemented.**

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Staggered UI Hydration Authority (R153)**: Stage-based UI initialization via `hydrationLevel` (0-3). (Issue #153). **Status: Implemented.**
*   **Event & Alert Text Authority (R747)**: Local event prefixing with "**This device:**". (Issue #747)

### 4. Version Authority
*   **Current Release**: Aug.17.01.
*   **Source of Truth**: app/build.gradle versionName.
