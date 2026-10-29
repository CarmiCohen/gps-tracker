# Project Revolution: Relay Server Specifications (HARDENED)

## 1. Presence Authority & Scaling
- **WebSocket Tracking**: The server must maintain a real-time mapping of active `trackerId` and `monitorId` connections.
- **Node Synchronization**: If scaling horizontally (multiple server nodes), a **Redis Pub/Sub** backend is required to synchronize state bursts and violation events between nodes.
- **Presence Rules**: 
    - If a Sensor disconnects, the Relay identifies all authorized Monitors.
    - Real-time `SENSOR_OFFLINE` is pushed to active WebSockets.
    - **High-Priority FCM Data Message** is pushed to registered Monitor tokens not currently connected via WebSocket.

## 2. Shadow State (State Burst)
- **Differential Sync**: The server must accept a `lastKnownSequenceId` during the Monitor's handshake. 
- **Burst Payload**: The server returns only the telemetry and events that occurred *after* that sequence ID, plus the current live status.
- **Server arrival timestamp**: The server must append a `serverArrivalTs` (monotonic unix timestamp) to every packet received from the Sensor before forwarding it. This is the Monitor's authority for the timeline.

## 3. Notification Logic
- **FCM Data Messages**: All violation alerts must be sent as **FCM Data Messages** (not Notification messages) with `priority: high`. 
- **Payload Constraints**: Limit FCM payload to 4KB (Trigger-only). Include only `type`, `sequenceId`, and `timestamp`. The Monitor will fetch full forensic details via its socket upon waking up.

## 4. Sequence Integrity
- The Relay must enforce **Sequence ID Monotonicity**. If a packet arrives from a Sensor with a `sequenceId` lower than the last processed one (e.g. out-of-order delivery), it must be flagged as `isBuffered: true` before forwarding.
