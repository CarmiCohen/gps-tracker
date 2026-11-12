# Setup & Sound Options Specification (vNov12.3)

This document details the revived setup, maintenance, phone capability guides, and sound/audio configuration overlays available across the Sensor (`AdminActivity`) and Monitor (`MonitorActivity`) applications.

## 1. Maintenance & Clean Setup (`CleanSetupOverlay`)
Accessible via configuration settings in both applications, `CleanSetupOverlay` provides administrators and operators with tools to maintain device state and clean local storage:
- **Clear Home Points & Geofence**: Removes all defined geofence home points (Monitor only: shown only when the app passes the action, Nov12.3). The tracker receives the empty geofence; alarm settings stay unchanged (Nov8.13, ISSUE-NOV8.13-O54).
- **Reset Operational Statistics**: Resets telemetry counters, distance metrics, and packet transmission logs.
- **Trigger Full System Re-Initialization**: Resets internal database repositories and restarts foreground engine workers.

## 2. Phone Setup & Capabilities Guide (`PhoneSetupOverlay`)
The `PhoneSetupOverlay` guides users through essential device-specific permissions, hardware hardening, and background execution requirements:
- **Device Identification**: Automatically detects and displays device manufacturer and model (e.g., Samsung A15 budget tier vs Samsung S21FE flagship).
- **Location Permission Guide**: Directs users to set location access to "Allow all the time" and enable Precise Location.
- **Battery Optimization Exemption**: Directs users to set battery optimization to "Unrestricted" to prevent OS sleep termination.
- **Overlay Permission ('Appear on Top')**: Authorizes high-priority alarm overlays and siren annunciation.
- **Notifications, DND bypass, Full-Screen Intent (Android 14+)**: opens the matching system settings.
- **Monitor only**: Home Points item (`Open Map` opens the home point editor) and the remote alarm test. **Sensor only**: Notification Access (optional).
- Removed in Nov12.3 (ISSUE-NOV10.8-O76): Exact Alarms (none used), `Refresh Status` and `Diagnostics` (did nothing). The Sensor's own page is reworked by ISSUE-NOV10.8-O81.

## 3. Alarming & Audio Setup (`AlarmSoundOverlay` & `AlertManagementOverlay`)
- **Alarm Sound Overlay**: Configure siren wave types (`Siren`, `Chimes`, `Pulse`), test audio output in real-time, configure haptic vibration, override Silent/Do Not Disturb, force maximum volume, and adjust custom app volume mode.
- **Alert Management Overlay**: Granular user controls for all active violation categories (communication, geofence, device health, storage, and sentinel motion sensors) with `ALL ON` and `RESET ALL` bulk actions and master Global Mute.
