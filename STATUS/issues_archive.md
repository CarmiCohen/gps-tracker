# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 254**

## 1. Recent Hardening Phase (v9.0.4 - v9.3.0)
*   **Issue #049**: Corrected GlobalStatusBar mapping to use mode-aware location context (v9.2.6).
*   **Issue #044**: Standardized HUD status badges to reflect local device health (v9.2.3).
*   **Issue #030**: Proto Schema Duplication (R973). Consolidated all schemas into `app/src/main/proto` and deprecated the legacy `app/src/proto` path to prevent synchronization drift. (v9.3.0)
*   **Issue #400**: Map Metadata Alignment (R400). Re-anchored Bayesian Uncertainty status messages from the map center to the bottom-center metadata cluster. (v9.3.0)
*   **Issue #326**: Intelligent Uncertainty UX. Enriched Location Pending state with reasons (GPS_GAP, JAMMER) and implemented priority-based merging. (v9.2.2)
*   **Issue #018**: Stationary Anchor Hard-Lock. Implemented coordinate clamping in `LocationProcessor.kt` and propagated `isAnchorLocked` flag. (v9.2.1)
*   **Issue #048**: Viewer HUD Line Grayout. Differentiated Telemetry Age from GPS Age in status rows. (v9.2.0)
*   **Issue #029**: Viewer Status Line Restoration. Propagated local telemetry to repository in Viewer mode. (v9.0.3)

## 2. Hardening Era Resolutions (v8.9.65 - v9.1.7)
*   **Issue #042**: Identity Sanitization Visibility. Implemented migration flag to notify UI of auto-sanitization events. (v9.3.0)
*   **Issue #041**: Identity Sanitization Hardening. Implemented R975 (Regex validation) and automatic storage purging. (v8.9.99)
*   **Issue #027**: Identity Persistence Hardening. Reinforced bulk save with atomic uniqueness validation. (v8.9.98)
*   **Issue #030-Legacy**: Proto Schema Discrepancy. Authoritative schema path moved to `app/src/main/proto`. (v8.9.96)
*   **Issue #032**: UI Refresh Consistency. Implemented `isForensicFresh` gate using `WATCH_DOG_UI_GRACE_MS`. (v8.9.96)
*   **Issue #038**: Adaptation Instability. Implemented 5s "Adaptation Muzzle" for A15 polling changes. (v8.9.94)
*   **Issue #037**: Viewer Display State Spam. Added `DisplayListener` to suppress proximity triggers during AOD cycles. (v8.9.94)
*   **Issue #036**: A15 Behavioral Flickering. Introduced hardened sensor mismatch and jitter thresholds. (v8.9.94)
*   **Issue #005**: Log Spillage Hardening. Static user agent and manual storage paths for osmdroid. (v8.9.91)
*   **Issue #028**: R924 Sunset. Purged legacy `VID_NOTES` identifiers. (v8.9.91)
*   **Issue #026**: Viewer ID Identity Reversion. Fixed draft commitment logic in `SettingsRepository`. (v8.9.87)
*   **Issue #025**: FGS Transition Timeout. Increased `UI_PULSE_TIMEOUT_MS` to 45s for Android 14+. (v8.9.86)
*   **Issue #024**: Accuracy Window Aliasing. Expanded bucket to 120s. (v8.9.85)
*   **Issue #023**: DataStore Binary Incompatibility. Reverted legacy tags to float and added high-precision doubles. (v8.9.84)
*   **Issue #022**: Deep-Link Cold-Start. Implemented intent-aware startup. (v8.8.6)
*   **Issue #021**: Map UI Infinite Loop. Fixed loop with single-point trail segments. (v8.9.82)
*   **Issue #020**: Map Centering Race. Introduced `localLockStatus` for user touch suspension. (v8.9.83)
*   **Issue #017**: SnapshotStateList Lock Failures. Replaced observable pools in map updates. (v8.9.81)
*   **Issue #016**: Main Thread Performance. Optimized trail rendering and offloaded startup I/O. (v8.9.80)
*   **Issue #014**: System-Wide Type Safety. Standardized telemetry fields to `Double`. (v9.1.7)
*   **Issue #015**: Coroutine Cancellation. Hardened lifecycle transitions against `CancellationException`. (v8.9.72)
*   **Issue #011**: Suppression Forensic Labeling. Added `suppressionNote` to `SentinelResult`. (v8.9.68)
*   **Issue #010**: A15 Coherence. Implemented physical reality gate for acoustic spikes. (v8.9.68)
*   **Issue #013**: Forensic UI Expansion. Exposed `proximityDebounceMs` and `vibrationRollingSum`. (v8.9.71)
*   **Issue #012**: Adaptive Proximity Debounce. Implemented scaling in `AppSensorManager`. (v8.9.71)
*   **Issue #R325**: Samsung A15 Accuracy Truncation. Optimized status row layout width (210dp). (v8.9.65)

## 3. Middle Era Resolutions (#100 - #199)
*   **Issue #199**: Toolchain Modernization. Upgraded to Java 17 and Android SDK 35. (v8.9.8)
*   **Issue #198**: GPS Availability Hardening. Shortened stall detection to 60s. (v8.9.8)
*   **Issue #197**: Database Schema Expansion (v38). Added `sitVzTs` to history. (v8.9.7)
*   **Issue #196**: Plunge Matching. Refined "Plunge" state machine. (v8.9.7)
*   **Issue #195**: Room Migration Audit. Implemented table reconstruction migration. (v8.9.6)
*   **Issue #194**: SIT Persistence Risk. Implemented acknowledged event pipeline. (v8.9.7)
*   **Issue #193**: Zombie Telemetry UX. Implemented "Ghost Mode" indicators. (v8.9.6)
*   **Issue #337**: Power Parity. Achieved forensic parity for `currentMa`. (Formerly #192)
*   **Issue #191**: Muzzle Window Race. Implemented deterministic Muzzle Handshake. (v8.9.6)
*   **Issue #190**: Xiaomi Autostart. Indeterminate status handling and boot grace. (v8.9.16)
*   **Issue #189**: Viewer Background Location. 10s background polling for Viewers. (v8.9.5)
*   **Issue #188**: Historical GPS Timestamp Loss. Added `gpsTs` to DB and sync. (v8.9.3)
*   **Issue #187**: Viewer-Side LocationProcessor State. Updated persistence loading. (v8.9.4)
*   **Issue #186**: SoT Hardening. Updated documentation to v8.9.2 baseline.
*   **Issue #185**: ViewerService Listener Completion. Remote-to-local trail persistence. (v8.9.2)
*   **Issue #183**: Legacy Branding Cleanup. Standardized to JD Green and logo. (v8.9.2)
*   **Issue #181**: GPS Stability Audit. Reliability metrics emitted every 10s. (v8.9.2)
*   **Issue #180**: Forensic Pipeline Verification. Verified verticalVelocity and SIT mapping. (v8.9.2)
*   **Issue #176**: R941 Statistics Persistence. Confirmed accumulation across restarts. (8.8.36)
*   **Issue #175**: R917 Update Smoothness. Verified `MY_PACKAGE_REPLACED` handling. (8.8.36)
*   **Issue #124**: GPS Revival Escalation. Implemented 5-minute retry loop. (8.8.31)
*   **Issue #115**: ViewModel Decoupling. Extracted feature-specific UseCases. (8.8.25)

## 4. Legacy Foundation Resolutions (#1 - #99)
*   **Issue #302**: Settings Verification. Removed role-based UI gating. (Formerly #30)
*   **Issue #301**: Alert Grace Period. Implemented 2s trigger delay. (Formerly #29)
*   **Issue #320**: Physical Tamper Race. Implemented 500ms Muzzle Window. (v8.8.21)
*   **Issue #300**: Xiaomi Traceability. UNKNOWN MIUI status guidance. (v8.8.21)
*   **Issue #299**: Geofence SOT Desync. Corrected JSON key mismatch. (v8.8.21)
*   **Issue #297**: Identity Collision. Hardened LogManager and DB v29. (v8.8.21)
*   **Issue #286**: Missing Thermal Throttling. Implemented `COOLING_GPS_POLLING_MS`. (8.8.17)
*   **Issue #281**: Sticky SIT State. Re-implemented `consumeSitDetected()`. (v8.8.13)
*   **Issue #280**: Timing Mismatch (Stall). Migrated to monotonic timestamps. (v8.8.12)
*   **Issue #274**: OS Restriction Monitoring. Integrated Standby Bucket detection.
*   **Issue #001**: Room Schema Divergence. Fixed missing columns and migrations. (v8.9.62)

---

## 🗺️ Legacy Issue Mapping (Authoritative Unification)
The following legacy IDs have been unified into the #300+ authoritative range.

| Legacy ID | Authoritative ID | Category / Description |
| :--- | :--- | :--- |
| #115 | #322 | Architectural Bloat: ViewModel Decoupling |
| #148 | #453 | Samsung A15 GPS Stalling |
| #180 | #340 | Samsung A15 Proximity Limitation |
| #190 | #455 | Xiaomi Autostart & Boot Resilience |
| #191 | #454 | Samsung A15 Proximity Flutter |
| #214-A | #325-B | Unified Accuracy Fallback Logic |
| #214-M | #347 | Stale Legacy Reference Migration |
| #219 | #332 | SNR-IMU Correlation Validation |
| #220 | #334 | Hindsight Trajectory Correction |
| #221 | #328-B | Bayesian Uncertainty / systemPulseRealtime |
| #224 | #329 | Forensic Ribbon Expansion (tiltIdx/baroIdx) |
| #227 | #327 | Hindsight Transition Smoothing |
| #496 | #326 | Intelligent Uncertainty UX Mapping |
| #497 | #327 | Hindsight Transition Smoothing |
| #337 | #337 | Power Parity: currentMa |
