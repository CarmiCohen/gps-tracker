# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 271**

## 1. Architectural Hardening & DI Finalization (v9.3.12)
*   **Issue #075**: Temporal Authority. Implemented skew-immune `isGpsFresh` logic in `DashboardUseCase` using receipt-time deltas, ensuring HUD/Map stability during clock drift.
*   **Issue #074**: Peer Activity HUD Authority. Refined `isPeerActive` logic in `GlobalStatusBar` to use role-specific freshness logic, ensuring Tracker-side badges track Viewer pulses exclusively. (Requirement R980)
*   **Issue #066**: TrackerService Hilt Refactor. Finalized the Hilt DI migration for the background service layer, eliminating legacy EntryPoint usage in the Tracker role. (Requirement R978)
*   **Issue #065**: Forensic Consolidation (#061). Standardized "Special Color" (Pink) logging via Room `LogDao` and `LogEntity` across all system modules. (Requirement R979)
*   **Issue #076**: Proto Precision Integrity. Verified `max_distance` and `max_accuracy` persistence in `SettingsRepository` and UI propagation. (Requirement R968)

## 2. Background Resilience & Permission UI (v9.3.11)
*   **Issue #059**: Permission Health Check UI. Implemented a dedicated Diagnostics interface for monitoring background resilience and hardware-specific adaptations (Xiaomi/Samsung). (Requirement R997)
*   **Issue #068**: Logcat Audit (Samsung Spam). Silenced `getPackageName` logcat spillage via cached identifiers in the service layer. (Requirement R996)

## 3. Synchronization & Signaling Hardening (v9.3.9)
*   **Issue #073**: Peer Visibility Asymmetry. Fixed signaling layer pulse acknowledgment on the Tracker device. (v9.3.9)
*   **Issue #072**: HUD Clock Skew Hardening. Transitioned HUD health logic to a Receipt-Time Authority model. (v9.3.8)

## 4. Historical Resolutions (v9.3.1 - v9.3.7)
*   **Issue #058**: TrackerService Initialization (R978). Migrated dependencies to `BaseMonitorService`. (v9.3.6)
*   **Issue #047**: Speed Zeroing Authority (R987). Verified immediate speed drop to 0.0 on GPS loss. (v9.3.6)
*   **Issue #046**: State Sync Audit (R986). Verified simultaneous HUD state transitions. (v9.3.6)
... [See historical logs for full 271 resolutions]
