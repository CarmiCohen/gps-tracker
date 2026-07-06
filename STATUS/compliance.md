# Compliance & Operational Requirements (Audit Baseline) - v9.1.1

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R982** | **Identity Locking Authority**: Trackers exclusively process packets from their linked `viewerId` (with Default Relaxation for pairing). | **Verified (v9.1.1)** |
| **R018** | **Stationary Anchor Hard-Lock**: coordinates clamped to `parkingAnchorPoint` when `stationaryProb > 0.9`. | **Verified (v8.9.78)** |
| **R014** | **Type Safety Authority**: All telemetry fields standardized to `Double` across engine and app. | **Verified (v8.9.75)** |
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
| **R810-A15**| **A15 High-Noise Profile**: Hardened sensor gates and vibration coherence. | **Verified (v8.9.94)** |
| **R832** | **Chair Sit Detection Engine**: Multi-sensor fusion (tilt/vibration/baro) for occupancy detection. | **Verified (v8.9.40)** |
| **R917** | **Update Smoothness**: Infrastructure for session recovery after package updates. | **Verified (v8.9.36)** |
| **R926** | **Service Launch Integrity**: Mandatory 2,000ms landing page pause before service launch. | **Verified (v8.9.40)** |
| **R944** | **Binary Signaling Efficiency**: Protobuf-based binary payload emission. | **Verified (v8.9.40)** |
| **R965** | **Sensor Processing Authority**: High-frequency sensor event processing offloaded to `AppSensorThread`. | **Verified (v8.9.64)** |
| **R966** | **Connectivity Integrity**: Reactive short-circuit reconnection trigger. | **Verified (v8.9.64)** |
| **R967** | **Foreground Transition Buffer**: 45s UI pulse window for Android 14+ FGS transitions. | **Verified (v8.9.86)** |
| **R970** | **Display State Integrity**: Suppression of telemetry artifacts during AOD transitions. | **Verified (v8.9.94)** |
| **R972** | **Forensic Staleness Authority**: 15s staleness gate for forensic fields. | **Verified (v8.9.95)** |
| **R973** | **Standardized Proto Path Authority**: `app/src/main/proto` is the sole schema directory. | **Verified (v8.9.96)** |
| **R974** | **Identity Uniqueness Authority**: Strict ID uniqueness check in `MainRepository`. | **Verified (v8.9.98)** |
| **R975** | **Identity Sanitization Authority**: Strict alphanumeric Regex validation and storage sanitization. | **Verified (v8.9.99)** |

## 2. Resolution Archive (Hardening Phase)

### 2.1. Hardening Phase Resolutions (v9.1.1)
*   **FIXED R982: Identity Locking Refinement** - Resolution: Enforced refined `viewerId` locking (R982) in `SignalingValidator` and `CommunicationManager`. Implemented "Lock-on-Non-Default" logic to support first-time pairing while ensuring strict peer authorization for established links. (Issue #042)

### 2.2. Hardening Phase Resolutions (v9.1.0)
*   **FIXED R799e: JD Vivid Green Branding** - Resolution: Migrated all Tracker-role and primary branding indicators to JD Vivid Green (#78BE20). Updated `Color.kt`, `colors.xml`, and branding documentation.

### 2.3. Hardening Phase Resolutions (v9.0.4)
*   **FIXED R799d: Viewer Branding Migration** - Resolution: Migrated all Viewer-role identity indicators from Orange to Cyan (#06B6D4). Updated `Theme.kt`, `Color.kt`, and all UI components.

**Full history available in [issues_archive.md](issues_archive.md).**
