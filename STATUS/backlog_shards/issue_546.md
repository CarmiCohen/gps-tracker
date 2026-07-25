# Issue #546: Signaling Handshake Instability

## 🎯 Status: Resolved (July.24.07)
**Category**: Connectivity / Signaling

---

## 📝 Description
WebSocket errors were detected during the initial relay handshake, particularly on budget hardware like the Samsung A15. This caused delays in telemetry synchronization.

## 🔍 Observations
- **Observation**: `EngineIOException: websocket error` during initial connection.
- **Impact**: Delayed telemetry sync and potential heartbeat failure.

## 🛠️ Resolution
- **Forensic Fix**: Implemented `isConnecting()` state in `SignalingProvider` and `CommunicationManager` to prevent "handshake storms" (redundant concurrent connection attempts).
- **Optimization**: Refined Socket.io options: reduced timeout to 30s for faster recovery, enabled `forceNew` for clean state, and optimized reconnection delays.
- **Result**: `EngineIOException` eliminated on Samsung A15. Initial state propagation is now stable and predictable.

## 🔗 References
- **Requirement**: Connectivity Reliability, R546 (Handshake Hardening)
- **Cycle**: July.24.07
