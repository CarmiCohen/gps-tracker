# Project Revolution: Monitor (Viewer) Requirements

## 1. Functional Overview
The Monitor is a user-centric application designed for visualization, alert management, and remote interaction. Its primary architectural constraint is extreme battery preservation.

## 2. Operating Model: Passive Background / Active Foreground
- **Active (Foreground)**: 
    - Full-speed WebSocket connection to the Relay for real-time Sensor tracking.
    - **Self-Tracking**: Continuous GNSS monitoring of the user's own position to show "You" vs "Them" on the map.
    - **Local Trail**: Temporary persistence of the user's own path for navigation reference.
- **Passive (Background)**: 
    - **Zero Polling**: All network sockets and GPS listeners are terminated immediately upon `onStop()`.
    - **Push Notifications**: Relies on FCM (Firebase Cloud Messaging) for critical violation alerts. Deep sleep is maintained until a real event occurs.

## 3. Visualization & Interaction (The UI)
- **Top Header Line & Control Menu**: Upper header in `MonitorActivity` providing Setup, Settings, and Disconnect controls.
- **Monitor Setup & Relay URL**: Accessible via the Setup button in the header; displays Tracker ID, Monitor ID, Relay URL, Version, and Relay Status, with inline URL editing capability.
- **Exhaustive Map Engine & Tools Specification**: For complete documentation of all preserved pre-revolution map features (dual trails, home points ADD/DEL, trail CLEAR/SAVE/LOAD, geofence boundary circles, jump/out-of-range markers, replay cursor, stationary anchor badge, and map controls overlay), see [MAP_FUNCTIONALITY_SPEC.md](MAP_FUNCTIONALITY_SPEC.md).
- **Hybrid Map**:
    - High-fidelity rendering of the **Sensor's** trail (remote).
    - Real-time display of the **User's** current location/trail (local).
- **Map Camera Logic & Control Tools**:
    - **Follow Mode**: Automatically centers map on Sensor telemetry updates.
    - **Map Settings Wheel**: Toggle button (`MapSettingsToggle`) for map control overlay.
    - **Map Tools**: Dedicated **Zoom In ("+")**, **Zoom Out ("-")**, **Center Tracker ("TRACKER")**, **Center Viewer ("VIEWER")**, **Out-of-Range Toggle ("OUT")**, **Jump Alert Toggle ("JUMP")**, **Import ("LOAD")**, **Export ("SAVE")**, **Add Home Point ("ADD")**, **Delete Home Point ("DEL")**, **Geofence Toggle ("FENCE")**, and **Clear Trails ("CLEAR")**.
- **Standardized HUD Status Bar & Dual Lines**:
    - Compact 2-line monospaced status bars (`HudStatusBar`) reflecting dual data sources:
      - **Line 1 (Local Device)**: Reflects local device data (`SELF`), with local GPS status LED in **Cyan (`#06B6D4`)**.
      - **Line 2 (Sensor / Tracker)**: Reflects remote sensor data (`TRK`), with sensor GPS status LED in **Green (`#10B981`)** when connected.
    - Hardened LED evaluation logic (`isRelayConnected = isInternet || isConnected`) for accurate SRV, GPS, TRK/VWR, DAT, and WDG indicators.
- **Dual Centering Map Buttons & Auto-Centering Logic**:
    - **`SENSOR` Button**: Centers map on the remote Tracker/Sensor location (Green icon).
    - **`LOCAL` Button**: Centers map on the local Monitor device location (Cyan icon).
    - **Connection-Aware Centering**: When disconnected, local line reflects local GPS status and map auto-centers around the monitor. When connected, each GPS LED represents the relevant device, and the map auto-centers around the known device (prioritizing the Sensor if both are known, unless overridden by user via `SENSOR` or `LOCAL` buttons).
- **Status Dashboard**: Sensor health metrics (Battery %, Temp, Signal).
- **Local Connection Awareness**: Monitor distinguishes between Relay failure and local device internet loss.

## 4. Reliability & Storage
- **Sticky Foreground Session**: `MonitorSessionService` operates as `START_STICKY` with reactive `dataStore.relayUrl` collection for continuous background telemetry ingestion.
- **Safety Pulse**: 30m background check-in via WorkManager.
- **Storage Health**: Monitor keeps the newest 50,000 trail rows of each tracker and prunes local user trails after 48h. If storage < 100MB, it stops recording local paths.
- **DND Bypass**: Optional capability to override Do Not Disturb for critical stolen vehicle sirens.

## 5. Permission Scope
- `INTERNET`
- `ACCESS_FINE_LOCATION` (Foreground Only)
- `POST_NOTIFICATIONS` (Critical for background alerts)
- `VIBRATE`
- `ACCESS_NOTIFICATION_POLICY` (For DND bypass)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- *Explicitly NOT required*: `ACCESS_BACKGROUND_LOCATION`, `RECORD_AUDIO`, `RECEIVE_BOOT_COMPLETED`.
