# Issue #541: Inefficient Telemetry Serialization (Direct Binary Flow)

## 🎯 Status: Resolved (July.24.06)
**Category**: Telemetry / Performance

---

## 📝 Description
High-frequency telemetry (up to 10Hz) was causing significant CPU and memory overhead due to JSON serialization and parsing on both the Tracker and Viewer. 

## 🛠️ Resolution
- **Direct Binary Flow**: Hardened the Protobuf binary path for Tracker-to-Viewer telemetry.
- **Path Prioritization**: Updated `ConnectivitySuite` and `CommunicationManager` to prioritize `location_update_bin` (Protobuf) over the legacy JSON path.
- **Bypass Overhead**: Raw bytes are now dispatched directly via `onBinaryUpdate` to `RemoteStatusRepository`, bypassing the `JSONObject` allocation hot-path.

## 🔗 References
- **Requirement**: R541 (Direct Binary Flow)
- **Cycle**: July.24.05 / July.24.06
