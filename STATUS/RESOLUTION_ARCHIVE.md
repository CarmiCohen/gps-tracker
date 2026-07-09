# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 260**

## 1. Recent Hardening Phase (v9.3.1 - v9.3.6)
*   **Issue #058**: TrackerService Initialization (R978). Finalized Hilt migration by moving all common dependencies to `BaseMonitorService`. Eliminated `EntryPointAccessors` in services, receiver, and application class. (v9.3.6)
*   **Issue #047**: Speed Zeroing Authority (R987). Verified immediate speed drop to 0.0 on GPS loss in Viewer HUD. (v9.3.6)
*   **Issue #046**: State Sync Audit (R986). Verified simultaneous Tracker/Viewer HUD state transitions under load. (v9.3.6)
*   **Issue #039**: Identity Rejection Feedback (R977). Implemented explicit UI feedback (Toasts) and persistent logging for identity collisions. (v9.3.4)
*   **Issue #042**: Identity Sanitization Visibility (R976). Implemented migration flag and AlertDialog to notify UI of auto-sanitization events. (v9.3.2)
*   **Issue #055**: Issue History Recovery. Restored 185 legacy resolutions from `compliance_archive.md`. (v9.3.0)
*   **Issue #054**: Requirement ID Collision. Audited and corrected overloaded Issue #326 in compliance manifest. (v9.3.0)

## 2. Early Hardening Phase (v9.0.4 - v9.3.0)
*   **Issue #049**: Corrected GlobalStatusBar mapping to use mode-aware location context (v9.2.6).
*   **Issue #044**: Standardized HUD status badges to reflect local device health (v9.2.3).
*   **Issue #030**: Proto Schema Duplication (R973). Consolidated all schemas into `app/src/main/proto`. (v9.3.0)
*   **Issue #400**: Map Metadata Alignment (R400). Re-anchored Bayesian Uncertainty status messages to bottom-center. (v9.3.0)
*   **Issue #326**: Intelligent Uncertainty UX. Enriched Location Pending state with reasons. (v9.2.2)
*   **Issue #018**: Stationary Anchor Hard-Lock. Implemented coordinate clamping in `LocationProcessor.kt`. (v9.2.1)
*   **Issue #048**: Viewer HUD Line Grayout. Differentiated Telemetry Age from GPS Age in status rows. (v9.2.0)
*   **Issue #029**: Viewer Status Line Restoration. Propagated local telemetry to repository in Viewer mode. (v9.0.3)

## 3. Hardening Era Resolutions (v8.9.65 - v9.1.7)
*   **Issue #041**: Identity Sanitization Hardening. Implemented R975 (Regex validation). (v8.9.99)
*   **Issue #005**: Log Spillage Hardening. Static user agent and manual storage paths for osmdroid. (v8.9.91)
*   **Issue #014**: System-Wide Type Safety. Standardized telemetry fields to `Double`. (v9.1.7)
*   **Issue #043**: Migration Integrity Audit (R985). Verified and hardened v53 Room database migration. (v9.1.7)
... [See historical logs for full 260 resolutions]
