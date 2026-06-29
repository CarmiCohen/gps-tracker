# Compliance & Operational Requirements (Audit Baseline) - v8.9.49

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions (Issue #1 to #199), see [compliance_archive.md](compliance_archive.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R338** | **Ghost Mode UI Staleness**: Visual staleness indicators (dimming) applied strictly at 10s. | **Verified (v8.9.49)** |
| **R568a** | **Last Relay Traffic Monotonic Timestamp**: Use of `elapsedRealtime` for zombie connection detection. | **Verified (v8.9.40)** |
| **R729** | **Behavioral Debouncing & Muzzle Hardening**: Unified timing gates for alert suppression (Issue #191). | **Verified (v8.9.40)** |
| **R730** | **Unified Vibration Floor Update (EMA)**: Dynamic floor tracking for environmental adaptation. | **Verified (v8.9.40)** |
| **R800** | **Unified Back Navigation**: Standardized `BackHandler` usage in `AlarmActivity`. | **Verified (v8.9.40)** |
| **R805** | **Map Marker Color Standardization**: Integration of Purple500 for marker categorization. | **Verified (v8.9.40)** |
| **R832** | **Chair Sit Detection Engine**: Multi-sensor fusion (tilt/vibration/baro) for occupancy detection. | **Verified (v8.9.40)** |
| **R853** | **Atomic HomePoint Updates**: Support for atomic bulk updates of home points. | **Verified (v8.9.40)** |
| **R854** | **Siren Master Control**: Unified UI grouping for siren alert management. | **Verified (v8.9.40)** |
| **R865** | **Unified Identity Green**: Branding color Green explicitly integrated as primary theme. | **Verified (Audit v8.9.40)** |
| **R866** | **Branding Accuracy**: JD Branding Green matches exactly #367C2B. | **Verified (Audit v8.9.40)** |
| **R880** | **Evidence-based Parking Exit**: Hardened behavioral state transitions in `TrackerStateManager`. | **Verified (v8.9.40)** |
| **R922** | **Role-aware LED Logic**: Tracker LEDs reflect local health; Viewer gates by peer pulse. | **Verified (v8.9.40)** |
| **R925** | **Landing Page Pause**: Mandatory 2,000ms pause during session auto-transition. | **Verified (v8.9.40)** |
| **R926** | **Service Launch Integrity**: Background service launched following auto-transition delay. | **Verified (v8.9.40)** |
| **R944** | **Binary Signaling Efficiency**: Protobuf-based binary payload emission. | **Verified (v8.9.40)** |
| **R945** | **Reactive System State Flows**: Cold-to-hot reactive flows for hardware/system state monitoring. | **Verified (v8.9.40)** |

## 2. Resolution Archive

### 2.1. Hardening Phase Resolutions (v8.9.49)
*   **FIXED Issue #427: UX Inconsistency - Status Badge Staleness** - Resolution: Aligned `WATCH_TIMEOUT_MS` and `WATCH_DOG_UI_GRACE_MS` (10s) with R338 mandate.
*   **FIXED Issue #428: R338 Compliance Gap (Data Healthy Badge)** - Resolution: Synchronized DAT badge with 10s telemetry gate.

### 2.2. Hardening Phase Resolutions (v8.9.48)
*   **FIXED Issue #438: Issue ID Mismatch (Power Forensics)** - Resolution: Unified all `currentMa` references to authoritative Issue #337.
*   **FIXED Issue #425: R865 Color Non-Compliance** - Resolution: Swapped Emerald500 for authoritative BrandJd (#367C2B).

### 2.3. Hardening Phase Resolutions (v8.9.44)
*   **FIXED Issue #430: Zeroing Baseline Asymmetry** - Resolution: Aligned `BARO_ZEROING_INTERVAL_MS` (300s) with the `PASSIVE_ZEROING_STATIONARY_MS` baseline.
*   **FIXED Issue #437: Acoustic Floor Calibration Logic** - Resolution: Aligned `ACOUSTIC_FLOOR_MIN_DB` (50dB) with absolute safety gate.

### 2.4. Hardening Phase Resolutions (v8.9.43)
*   **FIXED Issue #435: Hindsight Buffer Desync** - Resolution: Expanded buffer to 10 points for forensic parity.
*   **FIXED Issue #436: Stationary GPS Pulse Asymmetry** - Resolution: Aligned persistence interval with polling.
*   **FIXED Issue #426: Logic Regression - GPS Staleness Coupling** - Resolution: Decoupled GPS health from peer heartbeat.
