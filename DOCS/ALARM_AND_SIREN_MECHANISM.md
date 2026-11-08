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

## 3a. What Is Built (Nov7.3, ISSUE-NOV5.2-O11 group 1, slice 1)
- **Monitor alarms**: `Tracker offline` (no update for 35 seconds, switch `Peer Connection Offline`), `Tracker power disconnected` (`Power Disconnected / Unplugged`), `Tracker battery low` below 20%, ending at 22% (`Low Battery Level`), and `Tracker temperature high` above 46°C, ending at 44°C (`High Device Temperature`). Global Mute suppresses all of them. The other switches do not drive alarms yet.
- **Until acknowledged** (user decision, 2026-11-07): an alarm shows full screen and sounds the siren until `ACKNOWLEDGE ALARM`, even after its condition clears, when it is marked `(cleared)`. An acknowledged alarm is raised again only after its condition has cleared. There is no 30-second auto-stop or lockout.
- **Where it runs**: `MonitorAlarmEngine`, started by `MonitorSessionService` while a tracker is paired, so alarms work in the background. The alarm notification uses the `monitor_alarm` channel and opens `AlarmOverlayActivity`.
- **Siren**: `SirenPlayer` synthesizes the `Siren`, `Chimes`, and `Pulse` waves and follows the Siren & Audio settings. `Force Maximum` raises the alarm stream to maximum and restores it afterwards.
- **Stealth**: the Sensor has no audio or vibration code, and `SensorStealthGuardTest` keeps it that way.
- **Not built yet**: communication alarms, Sensor-detected violations, geofence, microphone, lift, and the remote siren. See `DOCS/issues.md`.
- **Built since**: GPS jump (Nov7.7), GPS signal loss and relay and internet alarms (Nov7.x), and the geofence breach (`Geofence boundary breach`, switch `Geofence Boundary Breach` = `AlertSettings.distance`): Nov8.12, rebuilt in Nov8.13 so the Sensor checks the user's Home Points (ISSUE-NOV8.13-O51). Microphone, lift, and the remote siren are still not built.

## 4. Persistence & DataStore Storage
- `AlertSettings` and `sirenType` are serialized and persisted securely via `SensorDataStore` and `MonitorDataStore` using `kotlinx.serialization` across application restarts and reboots.
