# Chat Session Export - Oct22.12

## 📅 Date: Oct 22, 2024
## 🎯 Focus: ISSUE-OCT22.0-F3 & ISSUE-OCT22.0-F4 investigation.

### 🛠️ Investigation Summary

#### ISSUE-OCT22.0-F3: Connection State Asymmetry & Telemetry Leak
*   **Root Cause Analysis**:
    *   `SignalingValidator.shouldProcessLocationUpdate` in Viewer mode currently accepts all packets from the target Tracker ID without verifying the `viewer_id` intended by the Tracker.
    *   In Tracker mode, the validator accepts any Viewer if the Tracker is still using the default "V" ID due to the `isDefault(ownViewerId)` bypass.
    *   This leads to a "leak" where a Viewer configured with a mismatched ID (e.g., V2) still receives and processes pulses from a Tracker (on V), erroneously showing a GREEN "TRK" badge.
*   **Proposed Fix**:
    *   Modify `SignalingValidator.shouldProcessLocationUpdate` to strictly enforce `viewer_id` parity for both roles.
    *   Ensure `RemoteStatusRepository` only updates connectivity state if the ID validation passes.

#### ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Failure
*   **Ping Values**: Verified `CommunicationManager` calculates RTT in `handlePongRelay`, but the UI might be displaying stale or zeroed values if the peer signal is not updating the activity timestamps correctly for the HUD.
*   **Sensor Data (Noise/Lux)**: `TelemetryMapper` contains mapping logic for `noise_idx` and `lux_idx`. The failure likely resides in the transmission layer (either missing in JSON payload or omitted in Protobuf encoding).
*   **Violation Propagation**: Investigation shows that `ConnectivitySuite` and `CommunicationManager` lack specific handlers for violation event relays. Violations occurred on the Tracker are not being emitted as signaling events, thus they never reach the Viewer.

### 📂 Files Reviewed
*   `app/src/main/java/com/gps19/app/ConnectivitySuite.kt`
*   `core/engine/src/main/java/com/gps19/core/engine/SignalingValidator.kt`
*   `core/engine/src/main/java/com/gps19/core/engine/SignalingConstants.kt`
*   `app/src/main/java/com/gps19/app/CommunicationManager.kt`
*   `app/src/main/java/com/gps19/app/RemoteStatusRepository.kt`
*   `app/src/main/java/com/gps19/app/MainViewModel.kt`
*   `app/src/main/java/com/gps19/app/HudMapper.kt`
*   `app/src/main/java/com/gps19/app/TelemetryMapper.kt`
*   `core/engine/src/main/java/com/gps19/core/engine/SignalPayloadGenerator.kt`
*   `app/src/main/java/com/gps19/app/ViewerScreen.kt`
*   `app/src/main/java/com/gps19/app/TelemetryRepository.kt`
*   `core/engine/src/main/java/com/gps19/core/engine/SmartSignalingDispatcher.kt`

### 🚀 Next Steps
1.  Harden `SignalingValidator.kt` to enforce ID parity.
2.  Update `SignalPayloadGenerator.kt` and `CommunicationManager.kt` to include and handle sensor telemetry and violation relays.
3.  Audit `AppSignalingEncoder.kt` for Protobuf field completeness.

**STOP ALL PROCESSING.**
