# Project Revolution: Feature & Function Matrix

This matrix defines the definitive placement of system functions in the revolutionized architecture.

| Feature / Function | Sensor (Tracker) | Monitor (Viewer) | Core Shared | Notes |
| :--- | :---: | :---: | :---: | :--- |
| **GPS Acquisition (Sensor)** | **Primary** | No | - | 1Hz - 10Hz, background persistent. |
| **GPS Acquisition (User)** | No | **Active** | - | Foreground only; for "You vs Them" display. |
| **Kalman Filtering** | Yes | No | **Yes** | Moved to Kotlin in `:core-shared`. |
| **Spatial Logic (Geofence)** | Yes | No | **Yes** | Moved to Kotlin in `:core-shared`. |
| **Violation Detection** | **Authority** | No | Yes | Sensor identifies; pushes to Relay. |
| **Alarm Annunciation** | No | **Primary** | - | Monitor plays siren/vibrates/shows overlay. |
| **Trail Rendering (Maps)** | No | **Primary** | - | High-fidelity; includes User trail. |
| **Live Debug Logs** | **Admin HUD** | No | Yes | Real-time scroll on Sensor screen. |
| **Forensic Audit** | **Full** | Active | Yes | SpillBuffer used on both for diagnostics. |
| **Remote Commands** | Receive/Exec | **Initiate** | Yes | e.g., "Zero Chair", "Reboot". |
| **Native C++ Code** | **DELETED** | **DELETED** | - | Replaced by 100% optimized Kotlin. |

## Key Architectural Shift
1. **Background Presence**: Only the Sensor maintains a persistent background state. The Monitor is a standard interactive app that cleans up all resources (GPS/Socket) when the user closes it.
2. **Alert Delivery**: Background alerts for the Monitor are delivered via **Push Notifications (FCM)** rather than active polling, ensuring maximum battery life.
3. **Admin Console**: The "Headless" Sensor still provides a minimalist HUD for on-site setup, calibration, and real-time log monitoring.
