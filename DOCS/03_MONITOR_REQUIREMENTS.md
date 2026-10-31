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
- **Hybrid Map**:
    - High-fidelity rendering of the **Sensor's** trail (remote).
    - Real-time display of the **User's** current location/trail (local).
- **Map Camera Logic**:
    - **Follow Mode**: Automatically centers on Sensor.
    - **Manual Override**: Panning disables Follow Mode; "Center on Sensor" button restores it.
- **Status Dashboard**: Sensor health metrics (Battery %, Temp, Signal).
- **Local Connection Awareness**: Monitor distinguishes between Relay failure and local device internet loss (Point 40).

## 4. Reliability & Storage
- **Safety Pulse**: 30m background check-in via WorkManager.
- **Storage Health**: Monitor prunes local user trails after 48h. If storage < 100MB, it stops recording local paths (Point 45).
- **DND Bypass**: Optional capability to override Do Not Disturb for critical stolen vehicle sirens (Point 26).

## 5. Permission Scope
- `INTERNET`
- `ACCESS_FINE_LOCATION` (Foreground Only)
- `POST_NOTIFICATIONS` (Critical for background alerts)
- `VIBRATE`
- `ACCESS_NOTIFICATION_POLICY` (For DND bypass)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- *Explicitly NOT required*: `ACCESS_BACKGROUND_LOCATION`, `RECORD_AUDIO`, `RECEIVE_BOOT_COMPLETED`.
