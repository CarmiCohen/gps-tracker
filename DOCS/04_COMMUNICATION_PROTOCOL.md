# Project Revolution: Communication & Signaling Protocol

## 1. Overview
The communication protocol between the **Sensor** and the **Monitor** is designed for extreme efficiency, prioritizing binary formats for telemetry and lightweight JSON for commands.

## 2. Transport Layer
- **Relay Server**: Acts as a stateful message broker (Signaling Server).
- **Format**: 
    - **Protobuf (gRPC/Proto3)**: Used for high-frequency telemetry (Location, Sensors, Health).
    - **JSON**: Used for low-frequency commands, settings sync, and metadata.
- **Security**: Device ID + Secret Key authentication for both Sensor and Monitor.

## 3. Data Flow Models

### A. Telemetry Stream (Sensor -> Relay -> Monitor)
- **High Frequency**: Pushed by Sensor at a variable rate (1s to 30s based on motion state).
- **Delta Compression**: The Sensor only transmits fields that have changed significantly (e.g., Lat/Lng change > 5m, Battery > 1%).
- **Monitor Behavior**: The Monitor subscribes to this stream **only when in foreground**. Upon entering background, the subscription is dropped.

### B. "Alive" Check (Monitor -> Relay -> Sensor)
- **On-Demand Polling**: Instead of the Sensor constantly reporting "I'm alive," the Monitor issues a "Pulse Check" when the user opens the app.
- **Relay Presence**: The Relay tracks the Sensor's socket state. If the Sensor disconnects, the Relay immediately flags the status as "Offline" for the Monitor.

### C. Violation Events (Sensor -> Relay -> Monitor)
- **Immediate Push**: Critical alerts (Geofence exit, Tamper, Jamming suspicion) bypass standard telemetry queues and are sent with `High` priority.
- **Offline Buffering**: If the Sensor is offline during a violation, the event is saved to the `ForensicSpillBuffer` and flushed immediately upon reconnection.

### D. Remote Commands (Monitor -> Relay -> Sensor)
- **Command Set**:
    - `CALIBRATE_BASELINE`: Reset vibration/lux floors.
    - `SET_CONFIG`: Update sensing intervals or geofence radii.
    - `TRIGGER_SIREN`: Manual alarm activation.
- **Ack Cycle**: Every command requires an `ACK_TS` (Timestamped Acknowledgment) from the Sensor to be considered successful by the Monitor UI.

## 4. Message Schemas (Protobuf)
```proto
message RealtimeStatus {
  string id = 1;
  double lat = 2;
  double lng = 3;
  float accuracy = 4;
  int32 battery = 5;
  int64 gps_ts = 6;
  bool is_moving = 7;
  bool in_violation = 8;
  // ... other sensor data
}
```

## 5. Network Optimization
- **Radio Wakeup Suppression**: The Monitor will not trigger radio wakeups while the screen is off.
- **Heartbeat Scaling**: Heartbeat intervals scale based on power state (AC Power = 30s, Battery = 300s).
