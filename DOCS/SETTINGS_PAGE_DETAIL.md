# Settings Page Detailed Reference (v9.3.6)

This document provides a comprehensive breakdown of every field, toggle, and action available in the "Settings" overlay of the GPS Tracker application.

## 1. Primary Identity & Routing
These settings define how the device identifies itself and where it sends its data. Available in all modes (Tracker/Viewer).
- **Tracker ID**: A unique string used to identify the tracker's device. Default: "T". (Standardized per R182)
- **Viewer ID**: Identifies the monitoring phone. Default: "V". (Standardized per R182)
- **Relay URL**: The address of the Socket.io server used to relay data. Default: `https://gps-survival-relay.onrender.com`. (Issue #106)

### 🛡️ Identity Validation & Hardening (v9.3.6)
To ensure system integrity, the identity fields are subject to strict validation:
- **Alphanumeric Contract (R975)**: All IDs must be strictly alphanumeric.
- **Uniqueness Authority (R974)**: The system prevents the Tracker and Viewer from sharing the same ID to avoid routing loops.
- **Collision Feedback (R977)**: If a user attempts to save a conflicting or invalid ID, the system provides immediate UI feedback (Toast) and a persistent Forensic Log entry detailing the rejection. (Issue #039)
- **Auto-Sanitization (R976)**: During migration or app updates, malformed IDs are automatically reset to defaults. A "Sanitization Alert" (AlertDialog) notifies the user if this occurred to ensure they are aware of the identity change. (Issue #042)

## 2. Geofencing (Fence Distance)
- **Geofence Radius**: Defines the radius (in meters) around each "Home Point." If the tracker moves beyond this distance, an alarm is triggered. Supports **Predictive Geofencing** which calculates breach 2.0s (`GEOFENCE_PREDICTIVE_LOOKAHEAD_S`) ahead of time based on velocity.

## 3. Sub-Navigation Overlays
### A. Alerts (Alert Management)
- **Siren Master Control**:
    - **Global Mute**: Inhibits audio sirens while maintaining visual and logged violations.
- **Communication**: 
    - **Local Internet**: `INTERNET_LOSS_THRESHOLD_MS` (60s).
    - **Relay Offline**: Server unreachable despite active internet.
    - **Tracker/Viewer Offline**: Signal Loss monitoring (Tracker: 180s / Viewer: 30s).
    - **Jammer Detection**: `JAMMER_DETECTION_THRESHOLD_MS` (180s) for sustained GPS instability (Issue #315).
- **Location**: 
    - **Geofence**: Based on `GEOFENCE_BUFFER_MULT` (6.0) and predictive look-ahead.
    - **GPS Stalled**: `GPS_STALL_THRESHOLD_MS` (60s). Featuring **Escalated Revival** (Issue #124) with a 120s retry loop.
    - **GPS Gap**: `GPS_GAP_THRESHOLD_MS` (60s) fix age threshold.
- **Device Status**: 
    - **Power Tamper**: `POWER_DISCONNECT_DEBOUNCE_MS` (3s). (Issue #163)
    - **Low Battery**: `CRITICAL_BATTERY_THRESHOLD` (20%) or charge deficit.
    - **High Temp**: `MAX_SAFE_TEMPERATURE_CELSIUS` (46.0°C).
    - **Battery Health**: Monitors steep discharge (5% in 10m). (Issue #353)
- **System Integrity**:
    - **System Storage Low**: Thresholds: 50MB (Low) / 10MB (Critical). (Issue #71)
    - **Xiaomi System**: Monitors background permission status. Includes `XIAOMI_BOOT_GRACE_MS` (30s). (Issue #190)
    - **Xiaomi Manual Override**: Allows bypassing Xiaomi-specific permission checks for "Unknown" states.
- **Physical Sentinel**: 
    - **Tamper Alert**: Combined sensor fusion for hardware handling.
    - **Tilt Alert**: Detects orientation changes (> 15°).
    - **Acoustic Alert**: Ambient noise jump detection (40dB).
    - **Lift Alert**: Barometric pressure changes (> 0.8m).
    - **Chair Occupied**: Advanced seat-sensing pattern matching. (Issue #459 / Formerly #336-E)

### B. Sound (Sound Setup)
- **Siren Type**: Select between "Siren", "Chimes", or "Pulse".
- **Test Audio**: Verifies volume and sound profile for 3s (`TEST_ALARM_DURATION_MS`).
- **Volume Control**: Toggle between "System Control" and "App Control".
- **Behaviors**: 
    - **Vibration**: Haptic feedback during alarms.
    - **Override Silent Mode**: Forces audio even if the phone is set to silent/DND.
    - **Force Max Volume**: Ramps volume to 100% on alarm.

### C. Clean (Clean & Initialize)
- **Clear Home Points**: Removes all geofence origins.
- **Reset Statistics**: Clears session drops, uptimes, and max values.
- **Full Initialization**: Factory-level data reset, clears all logs/trails.

## 4. Navigation & Persistence
- **Unified Back = Commit**: Exiting the settings screen triggers a commit to `DataStore` and re-initializes services if routing parameters changed. (**Requirement R800**)

## 5. Summary of Actions
- **Phone Setup**: Accesses the Setup Guide for OS-level permissions.
- **Load Config**: Import JSON configuration for bulk updates.
- **Save Logs**: Exports forensic event history with **Log Spatial Anchors** and mandatory `role` field.
