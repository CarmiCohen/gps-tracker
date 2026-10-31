# Project Revolution: Reliability & Failure Modes

## 1. Monitor (Viewer) Connectivity Failure
### Scenario: Monitor cannot reach the Relay Server while in Foreground.
- **Detection**: Socket `onDisconnect` or Watchdog timeout (> 15s without Server Pulse).
- **Monitor Action**: 
    - Sound unique "Connection Lost" siren.
    - Show Red "RELAY OFFLINE" UI state.
    - Log `RELAY_OFFLINE` event to local ForensicBuffer.
- **Recovery**: Monitor enters aggressive reconnection loop (Exponential backoff starting at 2s).

## 2. Sensor (Tracker) Connectivity Failure
### Scenario: Sensor loses connection to the Relay Server.
- **Detection**: Sensor's persistent socket drops.
- **Sensor Action**: 
    - Buffer all telemetry locally in the `ForensicSpillBuffer`.
    - Log `NETWORK_OUTAGE` event locally.
    - **Note**: Sensor remains silent (no local alarm) as per "Silent Sensor" rule.
- **Relay Server Action**:
    - Mark Sensor state as "OFFLINE" in the shared database.
    - **Dispatch Alert**:
        - Send `SENSOR_OFFLINE` WebSocket packet to Monitor (if Monitor is foreground).
        - Send `SENSOR_OFFLINE` Push Notification (FCM) to Monitor (if Monitor is background).

## 3. Sensor "Silent" Failure (Connected but not Sensing)
### Scenario: Sensor is connected to relay but GPS/IMU has stalled.
- **Detection**: Sensor's internal "Heartbeat" detects no new GPS/IMU data for > 30s.
- **Sensor Action**: 
    - Log `STALL_DETECTED` to forensic buffer.
    - Attempt self-healing (Restart sensing drivers).
    - Push `SENSOR_STALL` event to Relay.
- **Monitor Action**:
    - Receives `SENSOR_STALL` event via Relay.
    - Sounds "Sensor Trouble" alarm on the Monitor device.
