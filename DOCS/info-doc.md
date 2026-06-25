# Viewer Info Page Layout Documentation (v8.9.37)

This document describes the modern "Telemetry-First" HUD and Dashboard layout implemented under the v8.9.37 architecture, focusing on forensic continuity, high availability, and real-time diagnostic visibility.

## 1. Top HUD (Heads-Up Display)
The HUD provides at-a-glance status of the primary tracking link and position health.
- **Link Index**: Color-coded indicator of the relay connection quality (RTT-aware).
- **Position Health**: 10s (`GPS_UI_FAIL_THRESHOLD_MS`) staleness gate. Dims to "Ghost Mode" (Issue #193) if data is stale.
- **Forensic Identification**: The build version (v8.9.37) is displayed between Ribbons and Log buttons for audit traceability.

## 2. Dashboard Sections
### A. GPS & Movement
- **GPS-Index**: Overall reliability score (0.0-1.0) based on age, accuracy, and satellite count.
- **Tr Accuracy**: Current 1-sigma error in meters.
- **Tr Max**: Worst-case accuracy recorded in the session (High-water mark). (Issue #214)
- **Avg SNR**: Average Signal-to-Noise Ratio across used satellites.

### B. Environment (Physical Sentinel)
- **Vibration/Tilt**: Real-time orientation and shock monitoring (Issue #282).
- **Noise/Lux**: Ambient environmental monitoring (Issue #286).
- **Lift**: Barometric vertical displacement monitoring.

### C. Power & Storage (v8.9.37 Hardening)
- **Battery Drain**: Real-time current in mA (`currentMa`). (Issue #272)
- **Storage**: Binary indicator for Low/Critical disk space (50MB/10MB).
- **Steep Discharge**: Latch for abnormal battery health events (Issue #221).

## 3. Interaction & Overlays
- **Ribbons Button**: Toggles the sparkline visualization for forensic trends.
- **Log Button**: Accesses the coordinate-aware event history with **Log Spatial Anchors**.
- **Settings**: Primary configuration access with role-aware visibility.
