# Settings Page Detailed Reference (v8.9.10)

This document provides a comprehensive breakdown of every field, toggle, and action available in the "Settings" overlay of the GPS Tracker application.

## 1. Primary Identity & Routing
These settings define how the device identifies itself and where it sends its data. Available in all modes (Tracker/Viewer).
- **Tracker ID**: A unique string used to identify the tracker's device. This ID defines the "Room" on the relay server. Default: "Ttk".
- **Viewer ID**: Identifies the monitoring phone. Default: "Cohen".
- **Relay URL**: The address of the Socket.io server used to relay data between the Tracker and Viewer. Default: `https://gps-survival-relay.onrender.com`.

## 2. Geofencing (Fence Distance)
- **Geofence Radius**: Defines the radius (in meters) around each "Home Point." If the tracker moves beyond this distance, an alarm is triggered. Supports **Predictive Geofencing** which calculates breach 2.0s (`GEOFENCE_PREDICTIVE_LOOKAHEAD_S`) ahead of time based on velocity. Available in all modes.

## 3. Sub-Navigation Overlays
The settings page is organized into specialized overlays to reduce visual clutter. All sub-settings are accessible in both Tracker and Viewer modes.

### A. Alerts (Alert Management)
Allows selective enabling/disabling of the alert portfolio, organized by category:
- **Siren Master Control**:
    - **Global Mute**: When enabled, violations are recorded and displayed in red on the UI, but the audio siren is inhibited.
- **Communication**: 
    - **Local Internet**: `INTERNET_LOSS_THRESHOLD_MS` (60s).
    - **Relay Offline**: Triggered when the server unreachable but local internet is active.
    - **Tracker Offline**: Heartbeat monitoring (`TRACKER_SIGNAL_LOSS_THRESHOLD_MS` 180s).
    - **Signal Loss**: Generic peer communication loss indicator.
    - **Jammer Detection**: `JAMMER_DETECTION_THRESHOLD_MS` (180s) for sustained GPS instability.
- **Location**: 
    - **Geofence**: Based on `GEOFENCE_BUFFER_MULT` (6.0) and `GEOFENCE_ACCURACY_EXPANSION_MULT`.
    - **GPS Stalled**: `GPS_STALL_THRESHOLD_MS` (60s) hardware chip freeze detection. Featuring **Escalated Revival** (Issue 124/198) with a 120s retry loop.
    - **GPS Gap**: `GPS_GAP_THRESHOLD_MS` (60s) fix age threshold.
- **Device Status**: 
    - **Power Tamper**: `POWER_DISCONNECT_DEBOUNCE_MS` (3s).
    - **Low Battery**: `CRITICAL_BATTERY_THRESHOLD` (20%) or charge deficit.
    - **High Temp**: `MAX_SAFE_TEMPERATURE_CELSIUS` (46.0°C).
    - **Battery Health**: Monitors steep discharge (5% in 10m `BATTERY_STEEP_DISCHARGE_WINDOW_MS`).
- **System Integrity**:
    - **System Storage Low**: Warns if internal storage drops below 50MB (`SYSTEM_STORAGE_LOW_THRESHOLD_MB`).
    - **Xiaomi System**: Monitors background permission status on MIUI/HyperOS devices. Includes `XIAOMI_BOOT_GRACE_MS` (30s).
    - **Xiaomi Manual Override**: Allows bypassing Xiaomi-specific permission checks. When enabled, the `UNKNOWN` background permission state is treated as valid, preventing false-positive alarms. Located in "Phone Setup" guidance.
- **Physical Sentinel**: 
    - **Tamper Alert**: Combined sensor fusion for hardware handling (`TRACKER_TAMPER`).
    - **Tilt Alert**: Detects orientation changes (> 15° `TILT_THRESHOLD_DEGREES`).
    - **Acoustic Alert**: Zero-lag ambient noise detection (40dB jump `ACOUSTIC_THRESHOLD_DB_JUMP`).
    - **Lift Alert**: Barometric pressure changes (> 0.8m `BARO_LIFT_THRESHOLD_METERS`).
    - **Chair Occupied**: Advanced seat-sensing pattern matching.

### B. Sound (Sound Setup)
- **Siren Type**: Select between "Siren", "Chimes", or "Pulse".
- **Test Audio**: Verifies the current volume and sound profile immediately (`TEST_ALARM_DURATION_MS` 3s).
- **Volume Control Mode**: Toggle between "System Control" and "App Control" (software slider).
- **Behaviors**: 
    - **Vibration**: Enable/disable haptic feedback during alarms.
    - **Override Silent Mode**: Forces audio even if the phone is set to silent/DND.
    - **Force Max Volume**: Automatically ramps volume to 100% when an alarm triggers.

### C. Clean (Clean & Initialize)
- **Clear Home Points**: Removes all geofence origins.
- **Reset Statistics**: Clears session drops, uptimes, and max values.
- **Full Initialization**: Performs a factory-level data reset, clears all logs/trails, and restarts the app state.

## 4. Navigation & Persistence
- **Draft Mechanism**: UI interactions modify a temporary draft state in `MainViewModel`.
- **Unified Back = Commit**: Exiting the settings screen (via system Back gesture) triggers the commit process, persisting settings to `DataStore` and re-initializing services (Tracker/Viewer) if critical routing parameters changed.

## 5. Summary of Actions
- **Phone Setup**: Accesses the Setup Guide for OS-level permissions.
- **Load Config**: Import a JSON configuration file to bulk-update settings.
- **Save Logs**: Exports the current forensic event history and telemetry indices for audit. Standardized to include coordinate-aware anchors (v8.9.10) and mandatory `role` field.
