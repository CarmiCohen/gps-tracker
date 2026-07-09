# Compliance & Operational Requirements (Audit Baseline) - v9.3.6

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R978** | **Service Component Injection**: Core service components MUST be field-injected via Hilt. | **Verified (v9.3.6)** |
| **R973** | **Standardized Proto Path Authority**: `app/src/main/proto` is the sole schema directory. | **Verified (v9.3.0)** |
| **R400** | **Map Metadata Alignment**: Status messages anchored to bottom-center with 80dp offset. | **Verified (v9.3.0)** |
| **R994** | **Screen-Off Optimization Authority**: GPS polling frequency throttled to 5s when screen is off. | **Verified (v9.2.9)** |
| **R993** | **Notification Throttling Authority**: Foreground service notification update throttling. | 🟡 **Pending Validation** |
| **R990** | **Anchor Lock Breakout**: Displacement-weighted monitor for breakout (#053 / #062). | 🟡 **Pending Validation** |
| **R989** | **HUD Freshness Duality**: Differentiation between Telemetry and GPS freshness (#052). | 🟡 **Pending Validation** |
| **R988** | **Binary Forensic Parity**: Binary telemetry contract maintains field parity (#051). | 🟡 **Pending Validation** |
| **R987** | **Speed Zeroing Authority**: Immediate speed drop to 0.0 on GPS loss (#047). | 🟡 **Pending Validation** |
| **R986** | **State Sync Audit**: Simultaneous Tracker/Viewer HUD state transitions (#046). | 🟡 **Pending Validation** |
| **R985** | **Migration Integrity Audit**: v53 database migration verification (#043). | 🟡 **Pending Validation** |
| **R968** | **Proto Precision Integrity**: Preservation of max_distance/max_accuracy in UI (#033). | 🟡 **Pending Validation** |
| **#031** | **Soak Test Monitoring**: 24-hour stability audit for stability gaps. | 🟡 **Pending Validation** |
| **#039** | **Identity Rejection UX**: UI feedback for ID collisions (#063). | 🟡 **Pending Validation** |
| **#042** | **Sanitization Visibility**: UI notification for auto-sanitized IDs (#067). | 🟡 **Pending Validation** |
| **R960** | **HUD Local Capability Grouping**: GlobalStatusBar groups fundamental local hardware indicators. | **Verified (v9.2.7)** |
| **R049** | **HUD Context Mapping Authority**: `GlobalStatusBar` implements mode-aware telemetry binding. | **Verified (v9.2.6)** |
| **R991** | **HUD Local Health Standardization**: Top-level status badges reflect local device health. | **Verified (v9.2.3)** |
| **R326** | **Intelligent Uncertainty UX**: Contextual reasons (GPS GAP, JAMMER) propagated to HUD. | **Verified (v9.2.2)** |
| **R990b** | **Stationary Anchor Hard-Lock**: coordinates clamped when `stationaryProb > 0.9`. | **Verified (v9.2.1)** |
| **R951** | **GPS Stability Audit**: Monotonic reliability audit for Viewer-side GPS fixes. | **Verified (v9.0.2)** |
| **R982** | **Identity Locking Authority**: Trackers exclusively process packets from their linked `viewerId`. | **Verified (v9.1.2)** |
| **R018** | **Stationary Anchor Hard-Lock**: coordinates clamped to `parkingAnchorPoint` when `stationaryProb > 0.9`. | **Verified (v8.9.78)** |
| **R014** | **Type Safety Authority**: All telemetry fields standardized to `Double` across engine and app. | **Verified (v9.1.7)** |
| **R182** | **Role Identity Standards**: Unique IDs with 'T' and 'V' prefixes for system-generated defaults. | **Verified (v8.9.53)** |
| **R325** | **Authoritative Spatial Anchoring**: `maxAccuracy` is the exclusive authority. Layout optimized for narrow devices. | **Verified (v8.9.65)** |
| **R332** | **Adaptive Jump Confidence**: High-SNR/Zero-Vibe reflection detection with 6-minute hold. | **Verified (v8.9.55)** |
| **R334** | **Uncertainty Hindsight**: Linear interpolation of accuracy during path reconstruction. | **Verified (v8.9.52)** |
| **R338** | **Ghost Mode UI Staleness**: Visual staleness indicators (dimming) applied at 35s. | **Verified (v9.0.3)** |
| **R441** | **Siren Timing Integrity**: Monotonic silence latches and 30s hardware protection auto-stop. | **Verified (v8.9.52)** |
| **R460** | **Bayesian Uncertainty Expansion**: Dynamic growth (up to 33.3m/s) during fix gaps. | **Verified (v8.9.52)** |
| **R747** | **Standardized Alert Titles**: Localized "This device" and simplified "Device" subtitles. | **Verified (v8.9.51)** |
| **R799d** | **Viewer Identity Color**: Viewer role identity color changed to Cyan (#06B6D4) system-wide. | **Verified (v9.0.4)** |
| **R799e** | **JD Vivid Green Branding**: Tracker branding migrated to JD Vivid Green (#78BE20). | **Verified (v9.1.0)** |
| **R970** | **A15 Jitter Stabilization**: Hardened sensor gates and 5s spatial muzzling for A15. | **Verified (v8.9.94)** |
| **R971** | **G990E Display Hardening**: Suppression of telemetry artifacts during AOD transitions. | **Verified (v8.9.94)** |
| **R832** | **Chair Sit Detection Engine**: Multi-sensor fusion (tilt/vibration/baro) for occupancy detection. | **Verified (v8.9.40)** |
| **R917** | **Update Smoothness**: Infrastructure for session recovery after package updates. | **Verified (v8.9.36)** |
| **R926** | **Service Launch Integrity**: Mandatory 2,000ms landing page pause before service launch. | **Verified (v8.9.40)** |
| **R944** | **Binary Signaling Efficiency**: Protobuf-based binary payload emission. | **Verified (v8.9.40)** |
| **R965** | **Sensor Processing Authority**: High-frequency sensor event processing offloaded to `AppSensorThread`. | **Verified (v8.9.64)** |
| **R966** | **Connectivity Integrity**: Reactive short-circuit reconnection trigger. | **Verified (v8.9.64)** |
| **R967** | **Foreground Transition Buffer**: 45s UI pulse window for Android 14+ FGS transitions. | **Verified (v8.9.86)** |
| **R972** | **Forensic Staleness Authority**: 15s staleness gate for forensic fields. | **Verified (v8.9.95)** |
| **R974** | **Identity Uniqueness Authority**: Strict ID uniqueness check in `MainRepository`. | **Verified (v8.9.98)** |
| **R975** | **Identity Sanitization Authority**: Strict alphanumeric Regex validation and storage sanitization. | **Verified (v8.9.99)** |

## 2. Resolution Archive (Hardening Phase)

### 2.1. Hardening Phase Resolutions (v9.3.6)
*   **FIXED #058: TrackerService Initialization** - Resolution: Migrated manual dependency injection in `TrackerService` and `ViewerService` to Hilt field injection. Standardized service component initialization using `Listener` interfaces and `initialize(CoroutineScope)` methods. (Requirement R978)

### 2.2. Hardening Phase Resolutions (v9.3.0)
*   **FIXED R973: Proto Schema Duplication** - Resolution: Deprecated legacy `app/src/proto` path and consolidated all schemas into the authoritative `app/src/main/proto` directory. (Issue #030)
*   **FIXED R400: Map Metadata Alignment** - Resolution: Re-anchored Bayesian Uncertainty status messages from the map center to the bottom-center metadata cluster. Implemented an 80dp vertical offset. (Issue #400)
*   **FIXED #055: Issue History Recovery** - Resolution: Restored 185 "lost" legacy resolutions from `compliance_archive.md`.
*   **FIXED #054: Requirement ID Collision** - Resolution: Audited and corrected overloaded Issue #326.

### 2.3. Hardening Phase Resolutions (v9.2.9)
*   **FIXED R994: Screen-Off Optimization Authority** - Resolution: Implemented dynamic GPS down-sampling to 5000ms when screen is off using `DisplayManager`. (Issue R994)

### 2.4. Hardening Phase Resolutions (v9.2.6)
*   **FIXED R049: HUD Context Mapping Authority** - Resolution: Corrected `GlobalStatusBar` mapping to use mode-aware location context. (Issue #049)

### 2.5. Hardening Phase Resolutions (v9.2.3)
*   **FIXED R991: HUD Local Health Standardization** - Resolution: Standardized HUD status badges to reflect local device health. (Issue #044)

### 2.6. Hardening Phase Resolutions (v9.2.2)
*   **FIXED R326: Intelligent Uncertainty UX** - Resolution: Enriched Location Pending state with reasons (`GPS_GAP`, `JAMMER`). (Issue #326)

**Full history available in [issues_archive.md](issues_archive.md).**
