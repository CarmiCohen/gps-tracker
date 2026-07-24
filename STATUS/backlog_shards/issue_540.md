# Issue #540: Signaling Rejoin Loop / IPC Congestion

## 🎯 Status: Resolved (July.24.04)
**Category**: Telemetry / Network Reliability

---

## 📝 Description
Under certain network instability conditions, the system would enter a rapid join/rejoin loop, causing IPC congestion and excessive traffic.

## 🛠️ Resolution
- Implemented `lastForceJoinTs` cooldown in `ConnectivitySuite` to gate rejoin attempts.
- Increased traffic staleness tolerance to prevent premature reconnection triggers.
- Hardened the `Socket.io` transport options to prioritize stable WebSocket connections.

## 🔗 References
- **Requirement**: R540 (Signaling Stability)
- **Cycle**: July.24.04
