# Alarming & Siren Mechanism (vNov4.30)

This document describes the critical alert system and Alarming Setup of the GPS Tracker & Sentinel system, ensuring theft, tamper, and system failure events are managed with precision.

## 1. Alert Management Setup (`AlertManagementOverlay`)
The system provides granular user control over all active violation and telemetry trigger conditions via `AlertManagementOverlay` (`:core-ui`):
- **Master Controls**: Global Mute (temporarily suppresses all local and remote alert triggers).
- **Bulk Actions**: `ALL ON` and `RESET ALL` quick toggles.
- **Alert Categories**:
  - **Communication & Link**: Local Internet Offline, Relay Disconnected, Peer Offline, GNSS Signal Loss, Jammer / GPS Jump Detection.
  - **Location & Geofence**: Geofence Boundary Breach, GPS Telemetry Stall, Long Telemetry Time Gap.
  - **Power & Device Health**: Power Disconnected, Low Battery (<20%), Steep Battery Discharge, High Temperature (>46°C).
  - **System Integrity**: System Storage Low.
  - **Sentinel Hardware Motion**: Tamper/Vibration Sensor (with sensitivity slider), Tilt Angle Detection (with sensitivity slider), Acoustic / Microphone Noise Sensor, Lift / Height Change Sensor.

## 2. Siren Sound & Audio Setup (`AlarmSoundOverlay`)
The audio synthesis and siren behavior configuration overlay provides:
- **Audio Testing**: Real-time `TEST SIREN AUDIO` / `STOP` capability.
- **Siren Wave Types**: Choice of `Siren`, `Chimes`, or `Pulse`.
- **Audio Behaviors**:
  - Haptic Device Vibration (`vibrationEnabled`)
  - Override System Silent / Do Not Disturb (`overrideSilence`)
  - Force Maximum Hardware Volume (`useMaxVolume`)
- **Volume Control Mode**: System Stream Control vs Custom App Volume (with 0%–100% volume slider).

## 3. Role-Based Stealth Authority (R872)
- **Tracker Mode (Sentinel)**: Operates in absolute stealth mode. Local audio synthesis and visual red alert flashes are suppressed at the engine level. Violations are transmitted to the Viewer only.
- **Viewer Mode (Watcher)**: Receives remote telemetry and violation alerts, forcing audio output and red full-screen overlays when authorized.

## 4. Persistence & DataStore Storage
- `AlertSettings` and `sirenType` are serialized and persisted securely via `SensorDataStore` and `MonitorDataStore` using `kotlinx.serialization` across application restarts and reboots.
