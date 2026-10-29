# Project Revolution: Connection & Polling Lifecycle

## 1. Monitor (Viewer) Connectivity Strategy
### A. Foreground Activation (OnStart)
- **Action**: Monitor initiates a WebSocket connection to the Relay.
- **Identity Sync**: Sends Monitor ID and target Sensor ID.
- **Differential State Burst**: The connect packet includes the `last_sequence_id` stored in the Monitor's local Room DB. The Relay returns ONLY the points and events that occurred since that ID, drastically reducing data usage for short background gaps.

### B. Map Camera Logic
- **Follow Mode**: By default, the map centers on the Sensor.
- **Manual Intervention**: If the user pans the map, "Follow Mode" is disabled. A "Center on Sensor" FAB (Floating Action Button) appears to re-enable it.

## 2. Sensor (Tracker) Connectivity Strategy
### A. Command Persistence
- **Rule**: Every remote command received from the Monitor (e.g., `CHANGE_INTERVAL`) must be successfully written to the Sensor's DataStore **before** the Acknowledgment (ACK) is sent back to the Relay. This ensures configuration durability across power cycles.

## 3. Multi-Monitor Support
- The system supports multiple Monitors per Sensor. The Relay Server handles the fan-out of telemetry to all active WebSocket subscribers and dispatches FCM notifications to all registered tokens for that `trackerId`.
