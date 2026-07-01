# Compliance & Operational Requirements (Audit Baseline) - v8.9.68

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions (Issue #1 to #199), see [compliance_archive.md](compliance_archive.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R182** | **Role Identity Standards**: Unique IDs with 'T' and 'V' prefixes for system-generated defaults. | **Verified (v8.9.53)** |
| **R325** | **Authoritative Spatial Anchoring**: `maxAccuracy` is the exclusive authority. Layout optimized for narrow devices (Samsung A15). | **Verified (v8.9.65)** |
| **R332** | **Adaptive Jump Confidence**: High-SNR/Zero-Vibe reflection detection with 6-minute hold. | **Verified (v8.9.55)** |
| **R334** | **Uncertainty Hindsight**: Linear interpolation of accuracy during path reconstruction. | **Verified (v8.9.52)** |
| **R338** | **Ghost Mode UI Staleness**: Visual staleness indicators (dimming) applied at 35s. | **Verified (v8.9.62)** |
| **R441** | **Siren Timing Integrity**: Monotonic silence latches and 30s hardware protection auto-stop. | **Verified (v8.9.52)** |
| **R460** | **Bayesian Uncertainty Expansion**: Dynamic growth (up to 33.3m/s) during fix gaps. | **Verified (v8.9.52)** |
| **R747** | **Standardized Alert Titles**: Localized "This device" and simplified "Device" subtitles. | **Verified (v8.9.51)** |
| **R810-A15**| **A15 High-Noise Profile**: Hardened sensor gates and vibration coherence for isolated A15 hardware. | **Verified (v8.9.68)** |
| **R832** | **Chair Sit Detection Engine**: Multi-sensor fusion (tilt/vibration/baro) for occupancy detection. | **Verified (v8.9.40)** |
| **R917** | **Update Smoothness**: Infrastructure for session recovery after package updates. | **Verified (v8.9.36)** |
| **R924** | **VID Notes Authority**: Button row displays `VID_NOTES` ("renumv") instead of version number. | **Verified (v8.9.65)** |
| **R926** | **Service Launch Integrity**: Mandatory 2,000ms landing page pause before service launch. | **Verified (v8.9.40)** |
| **R944** | **Binary Signaling Efficiency**: Protobuf-based binary payload emission. | **Verified (v8.9.40)** |
| **R965** | **Sensor Processing Authority**: High-frequency sensor event processing offloaded to `AppSensorThread`. | **Verified (v8.9.64)** |
| **R966** | **Connectivity Integrity**: Reactive short-circuit reconnection trigger (immediate `wakeUpRelay`). | **Verified (v8.9.64)** |

## 2. Resolution Archive (Hardening Phase)

### 2.1. Hardening Phase Resolutions (v8.9.68)
*   **FIXED Issue #011: Suppression Forensic Labeling** - Resolution: Implemented `suppressionNote` in `SentinelResult` and logging in `TrackerService` to provide transparency for hardware-profile muzzles.
*   **FIXED Issue #010: A15 Acoustic/Vibration Coherence** - Resolution: Implemented physical reality gate; acoustic spikes on A15 are suppressed if concurrent vibration is < 0.01g.
*   **FIXED Issue #013: A15 GPS Heartbeat Audit** - Resolution: Implemented "Warm Handoff" via `gpsManager.kickGps()` to prevent background clock stalls during state transitions.
*   **FIXED Issue #009: A15 Forensic Isolation Spikes** - Resolution: Increased acoustic jump thresholds to 55dB and optimized light EMA factors for Samsung A15.

### 2.2. Hardening Phase Resolutions (v8.9.65)
*   **FIXED Issue #R325: Samsung A15 Accuracy Truncation** - Resolution: Optimized layout width constraints to ensure authoritative accuracy visibility on narrow devices.
*   **FIXED Issue #461: Settings Uniqueness UI Feedback** - Resolution: Implemented error propagation from repository to UI via Toast.
*   **FIXED Issue #008: VID_NOTES Correction** - Resolution: Corrected note identifier to "renumv".

### 2.3. Hardening Phase Resolutions (v8.9.64)
*   **FIXED Issue #006: Samsung A15 Main Thread Jitter** - Resolution: Offloaded sensor callbacks to dedicated `HandlerThread`.
*   **FIXED Issue #007: Connectivity Rejoin Latency** - Resolution: Implemented reactive connection-lost triggers and enforced `websocket` transport.

### 2.4. Hardening Phase Resolutions (v8.9.62)
*   **FIXED Issue #001: Room Schema Divergence** - Resolution: Incremented DB to v51 and corrected historical migrations.
*   **FIXED Issue #002: GPS Status UI Mismatch** - Resolution: Increased failure thresholds to 35s to align with stationary GPS duty cycle.
*   **FIXED Issue #003: Main Thread Jitter (Davey)** - Resolution: Moved behavioral state computations to `Dispatchers.Default`.
*   **FIXED Issue #004: A15 Virtual Proximity Suppression** - Resolution: Refined manager to allow 'Far' transitions during motion in darkness.
*   **FIXED Issue #005: Map Provider Log Spillage** - Resolution: Silenced osmdroid debug logs.
*   **FIXED Issue #458: Tracker Role Ghost Mode Bug** - Resolution: Fixed timestamp propagation in GlobalStatusBar.
*   **FIXED Issue #459: Unicode Escape Regression** - Resolution: Fixed double-escaping in labels.
*   **FIXED Issue #460: Local Freshness Existence Logic** - Resolution: Relaxed check to include sensor-only telemetry.
