# System Source of Truth (SoT) - Aug.16.00 (Map & Startup Hardening)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Map Allocation Hardening (R182)**: (Added Aug.16.00) The system MUST eliminate allocation churn during map rendering. `MapOverlayManager` MUST reuse cached `GeoPoint` objects within `TrailPoint` and `ViolationPoint` to prevent GC thrashing and Startup ANR. `STARTUP_SETTLING_DELAY_MS` is increased to 10000ms to protect the main thread during first-frame rendering. (Issue #182). **Status: Implemented.**
*   **System Startup Hardening (R181)**: (Updated Aug.16.00) The system MUST defer 100Hz telemetry engine start and heavy osmdroid initialization until after a 10000ms settling period to prevent Binder exhaustion and `DeadSystemException` on resource-constrained environments. (Issue #181). **Status: Implemented.**
*   **Log Identity Integrity (R180)**: (Updated Aug.15.03) The system MUST utilize `localId` as the primary unique constraint for all log entries. `LogDao` MUST implement `OnConflictStrategy.IGNORE`. Legacy `UNIQUE` indices MUST be dropped to prevent `SQLiteConstraintException`. (Issue #180). **Status: Implemented.**
*   **High-Frequency Allocation Hardening (R179)**: (Added Aug.15.01) Throttled UI History Emitter (2Hz) MUST be used to decouple 100Hz telemetry from UI Flow emissions. (Issue #179). **Status: Implemented.**
*   **Forensic Flow Gating (R178)**: (Added Aug.15.01) Heavy data mapping MUST be gated by UI visibility. Transformation overhead MUST be eliminated when UI components are closed. (Issue #178). **Status: Implemented.**

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Staggered UI Hydration Authority (R153)**: (Added Aug.13.05) Stage-based UI initialization via `hydrationLevel` (0-3) to ensure frame-rate stability. (Issue #153). **Status: Implemented.**
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) Local event prefixing with "**This device:**". (Issue #747)

### 4. Version Authority
*   **Current Release**: Aug.16.00.
*   **Source of Truth**: app/build.gradle versionName.
