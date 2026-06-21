# Viewer Info Page Layout Documentation (v8.9.10)

This document describes the modern "Telemetry-First" HUD and Dashboard layout implemented under the v8.9.10 architecture, focusing on forensic continuity, high availability, and real-time diagnostic visibility.

## 1. Top HUD (Heads-Up Display)
The HUD provides at-a-glance status of the primary tracking link and position health.
- **Link Index**: Color-coded indicator of the relay connection quality.
- **Position Health**: 10s (`GPS_UI_FAIL_THRESHOLD_MS`) staleness gate. Dims to "Ghost Mode" if data is stale.
- **Forensic Identification**: The build version (v8.9.10) is displayed between Ribbons and Log buttons for audit traceability.

## 2. Dashboard Sections
### A. GPS & Movement
- **GPS-Index**: Overall reliability score (0.0-1.0).
- **Tr Accuracy**: Current 1-sigma error in meters.
- **Tr Max**: Worst-case accuracy recorded in the session.
- **Avg SNR**: Average Signal-to-Noise Ratio across used satellites.

### B. Environment (Physical Sentinel)
- **Vibration/Tilt**: Real-time orientation and shock monitoring.
- **Noise/Lux**: Ambient environmental monitoring.
- **Lift**: Barometric vertical displacement.

### C. Power & Storage (v8.9.10 Hardening)
- **Battery Drain**: Real-time current in mA (`currentMa`).
- **Storage**: Binary indicator for Low/Critical disk space.
- **Steep Discharge**: Latch for abnormal battery health events.

## 3. Interaction & Overlays
- **Ribbons Button**: Toggles the sparkline visualization.
- **Log Button**: Accesses the coordinate-aware event history (v8.9.10).
- **Settings**: Primary configuration access.
