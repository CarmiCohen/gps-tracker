# The Revolution: Mission Statement

## 1. Vision
To decouple the current monolithic GPS tracking system into a high-precision sensing engine and a battery-efficient monitoring interface. This split resolves architectural bottlenecks and eliminates startup performance violations (ISSUE-OCT28.26-F1).

## 2. Core Entities

### A. The Sensor (Tracker)
*   **Role**: Dedicated data producer and environmental sentinel.
*   **Operating Model**: Permanent Background Service (Headless).
*   **Environment**: Powered (no battery constraint).
*   **Responsibilities**: 
    *   Continuous GNSS/GPS acquisition.
    *   Spatial logic and Kalman filtering.
    *   Violation and alarm detection (Internal logic).
    *   Telemetry transmission to the relay.
*   **UI**: Minimalist Admin HUD for device naming, hardware calibration, and real-time diagnostic stream.

### B. The Monitor (Viewer)
*   **Role**: User interaction and status visualization.
*   **Operating Model**: Foreground-Only (Active on demand).
*   **Environment**: Mobile device (Battery sensitive).
*   **Responsibilities**:
    *   Visualizing the Sensor's trail and status.
    *   Issuing remote commands (e.g., "Zero Chair").
    *   Managing user notifications and alarms.
*   **Optimization**: Completely idle in the background. No GPS polling or background radio activity unless the user is actively viewing the monitor.

## 3. Key Success Metrics
- **Startup Latency**: < 200ms cold start for the Monitor UI.
- **Battery Impact**: < 1% hourly drain for the Monitor in idle state.
- **Complexity**: Clean separation of sensing logic (Sensor) from rendering logic (Monitor).
