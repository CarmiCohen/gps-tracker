# Compliance & Operational Requirements (Audit Baseline) - v8.9.96

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R018** | **Stationary Anchor Hard-Lock**: coordinates clamped to `parkingAnchorPoint` when `stationaryProb > 0.9`. | **Verified (v8.9.78)** |
| **R014** | **Type Safety Authority**: All telemetry fields standardized to `Double` across engine and app. | **Verified (v8.9.75)** |
| **R182** | **Role Identity Standards**: Unique IDs with 'T' and 'V' prefixes for system-generated defaults. | **Verified (v8.9.53)** |
| **R325** | **Authoritative Spatial Anchoring**: `maxAccuracy` is the exclusive authority. Layout optimized for narrow devices. | **Verified (v8.9.65)** |
| **R332** | **Adaptive Jump Confidence**: High-SNR/Zero-Vibe reflection detection with 6-minute hold. | **Verified (v8.9.55)** |
| **R334** | **Uncertainty Hindsight**: Linear interpolation of accuracy during path reconstruction. | **Verified (v8.9.52)** |
| **R338** | **Ghost Mode UI Staleness**: Visual staleness indicators (dimming) applied at 35s. | **Verified (v8.9.62)** |
| **R441** | **Siren Timing Integrity**: Monotonic silence latches and 30s hardware protection auto-stop. | **Verified (v8.9.52)** |
| **R460** | **Bayesian Uncertainty Expansion**: Dynamic growth (up to 33.3m/s) during fix gaps. | **Verified (v8.9.52)** |
| **R747** | **Standardized Alert Titles**: Localized "This device" and simplified "Device" subtitles. | **Verified (v8.9.51)** |
| **R810-A15**| **A15 High-Noise Profile**: Hardened sensor gates and vibration coherence. | **Verified (v8.9.94)** |
| **R832** | **Chair Sit Detection Engine**: Multi-sensor fusion (tilt/vibration/baro) for occupancy detection. | **Verified (v8.9.40)** |
| **R917** | **Update Smoothness**: Infrastructure for session recovery after package updates. | **Verified (v8.9.36)** |
| **R924** | **VID Notes Authority**: Button row displays `VID_NOTES`. | **OBSOLETE (v8.9.89)** |
| **R926** | **Service Launch Integrity**: Mandatory 2,000ms landing page pause before service launch. | **Verified (v8.9.40)** |
| **R944** | **Binary Signaling Efficiency**: Protobuf-based binary payload emission. | **Verified (v8.9.40)** |
| **R965** | **Sensor Processing Authority**: High-frequency sensor event processing offloaded to `AppSensorThread`. | **Verified (v8.9.64)** |
| **R966** | **Connectivity Integrity**: Reactive short-circuit reconnection trigger. | **Verified (v8.9.64)** |
| **R967** | **Foreground Transition Buffer**: 45s UI pulse window for Android 14+ FGS transitions. | **Verified (v8.9.86)** |
| **R970** | **Display State Integrity**: Suppression of telemetry artifacts during AOD transitions. | **Verified (v8.9.94)** |
| **R972** | **Forensic Staleness Authority**: 15s staleness gate for forensic fields. | **Verified (v8.9.95)** |
| **R973** | **Standardized Proto Path Authority**: `app/src/main/proto` is the sole schema directory. | **Verified (v8.9.96)** |

## 2. Resolution Archive (Hardening Phase)

### 2.1. Hardening Phase Resolutions (v8.9.96)
*   **FIXED Issue #030: Proto Schema Discrepancy** - Resolution: Audited and formalized `app/src/main/proto` as the authoritative path. Neutralized legacy `app/src/proto` folder to prevent split-brain updates.

### 2.2. Hardening Phase Resolutions (v8.9.95)
*   **FIXED Issue #032: UI Refresh Consistency** - Resolution: Implemented 15s forensic staleness gate (WATCH_DOG_UI_GRACE_MS) in `DashboardUseCase`.

### 2.3. Hardening Phase Resolutions (v8.9.94)
*   **FIXED Issue #036: A15 Behavioral Flickering** - Resolution: Hardened R810-A15 thresholds for sensor mismatch and jitter.
*   **FIXED Issue #037: Viewer Display State Spam** - Resolution: Implemented `DisplayListener` and R970 muzzling for Samsung AOD transitions.
*   **FIXED Issue #038: Adaptation Instability** - Resolution: Implemented 5s adaptation muzzle for GPS polling transitions.

### 2.4. Hardening Phase Resolutions (v8.9.89)
*   **OBSOLETE Requirement R924** - Resolution: Removed `VID_NOTES` identifier and its UI display in `HeaderBar` as per forensic request.

### 2.5. Hardening Phase Resolutions (v8.9.78)
*   **FIXED Issue #018: Tracker Behavior Stability (Hard-Lock)** - Resolution: Implemented stationary anchor logic in `LocationProcessor.kt` to eliminate coordinate drift.

**Full history available in [issues_archive.md](issues_archive.md).**
