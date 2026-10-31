# Project Revolution: Logic Refactor Blueprint

## 1. The Repository Split
| Legacy: MainRepository | Sensor (Tracker) | Monitor (Viewer) |
| :--- | :--- | :--- |
| **Storage** | ForensicSpillBuffer (Binary) | Room Database (SQLite) |
| **Logic** | Data Production & Buffering | Data Consumption & Caching |
| **Startup** | Instant (No DB Init) | Lazy (Map-linked) |

## 2. Connectivity Decoupling
- **Shared (`:core-network`)**: `SignalingTransport`, `ProtobufMapper`.
- **Sensor Service**: Proactive, persistent connection. Sends `Heartbeat` and `Telemetry`.
- **Monitor Activity**: Lifecycle-aware. Sends `AlivePoll`. Subscribes to `TrailStream`.

## 3. Sensing Logic (The Brain)
- Move `LocationSentinel`, `GtoEngine`, and `KalmanFilter` to `:core-shared`.
- **Optimization**: Port all remaining C++ math to Kotlin. 
- **Validation**: Sensor uses these to filter raw GPS. Monitor uses these to show the "User Trail."

## 4. Alarm Distribution
- **Sensor**: Detection only. Dispatches `ViolationEvent` via Relay. Remains SILENT.
- **Monitor**: Annunciation. Receives `ViolationEvent` and triggers Siren/Vibration.

## 5. Global Search & Destroy
Search for these patterns in the new project and replace them:
- `isTrackerMode` -> Deleted (Logic is app-specific).
- `AppRole` -> Deleted (Role is defined by the APK).
- `LandingActivity` -> Deleted (Start directly into HUD or Map).
- `externalNativeBuild` -> Deleted (100% Kotlin).
