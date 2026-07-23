# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 366**

## 1. Stealth & Startup Hardening (July.23.11)
*   **Tracker Stealth Violation (Audio Alarm)**: Hardened `AppAlarmManager.kt` to suppress `shouldPlaySiren` in tracker mode. Trackers now remain strictly silent even during system violations, preventing accidental detection.
*   **FGS Startup Stabilization (R406b)**: Moved `startServiceForeground()` to the Main-thread `onCreate` in `BaseMonitorService.kt`. This fixes the `ForegroundServiceDidNotStartInTimeException` that caused crash loops during automatic restoration from the landing page.

## 2. Hardware Hardening & Status Consistency (July.23.10)
*   **Issue #533: SRV Status Indicator Inconsistency**: Modified `CommunicationManager.kt` to proactively update `TelemetryRepository` on socket connection state changes (Connect, Disconnect, Reconnect, Error). This ensures the UI "SRV" badge reflects real-time status immediately.
*   **Issue #098: Step Detector Hardening**: Implemented explicit `ACTIVITY_RECOGNITION` permission check in `AppSensorManager.kt` before registering the Step Detector on Android 10+. This prevents hardware-level `fail(2)` (Permission Denied) errors.

## 3. Validation & Engine Hardening (July.23.09)
*   **Issue #533b: AnchorEvaluator Validation**: Implemented comprehensive unit tests verifying coordinate averaging, urban multipath suppression, and Safety Valve breakout behavior.
*   **Anchor Logic Hardening (R990c)**: Hardened the coordinate averaging logic to strictly exclude points outside the breakout threshold, preventing anchor "chase" and preserving escape sensitivity.

## 4. Telemetry Refinement & Documentation Audit (July.23.05)
*   **Issue #534**: Telemetry Pipeline Refinement. Standardized engine constants and signaling keys for improved event processing and viewer service synchronization.
*   **Issue #535**: Documentation Integrity Audit. Synchronized `SOT_MASTER_REQUIREMENTS.md`, `issues.md`, and technical guides to ensure documentation integrity.

## 5. Hardening & Finality (July.23.04)
*   **Issue #533**: Stationary Anchor Refinement. Implemented coordinate-averaging convergence using an 8-point sliding window buffer. Suppresses micro-drifts and "spaghetti" trails in urban canyons.
*   **Issue #532**: Type Safety Audit (R999). Upgraded telemetry pipeline to strict `Double` precision across all layers to prevent kinematic drift.
*   **Issue #531**: Acoustic Duty Cycle Logic Refinement. Fixed OS notification flickering by decoupling recording state from monitoring intent.
*   **Issue #529**: Geofence Reliability - Accuracy Recovery. Implemented grace logic in `PhysicsUtils` to suppress false "Visual Jump" alerts during accuracy stabilization.
*   **Issue #528**: DashboardUseCase Tombstone. Decommissioned orphaned use case and migrated logic to `DashboardStateProvider`.
*   **Issue #527**: Siren Persistence. Implemented DataStore-backed state restoration to ensure sirens resume after service restarts.
*   **Issue #526**: Power Optimization - Adaptive Sensor Sampling. Implemented two-tier power saving for logic and hardware when stationary.
*   **Issue #525**: State Audit - Forensic Propagation. Hardened end-to-end telemetry and fixed mapping bugs in local history ribbons.
*   **Issue #524**: UI Decoupling. Extracted UI formatting logic from the dashboard component into a dedicated provider.
*   **Issue #523**: Forensic Snapshot Consolidation. Implemented atomic immutable state evaluation for all 15+ forensic parameters.

... [See historical logs for full resolutions]
