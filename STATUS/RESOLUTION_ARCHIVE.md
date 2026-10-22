# Resolution Archive - GPS Tracker (Oct22.13)

## ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Failure (Oct22.13)
- **Resolved**: Oct22.13
- **Description**: Forensic sensor data (Noise/Lux/Vibe) and Ping (RTT) values were not reaching the Viewer HUD (showing 0). Tracker violations were also not mirroring on the remote Viewer.
- **Remediation**:
    - **Protobuf Schema**: Expanded `RealtimeStatus` in `app_settings.proto` to include raw forensic fields and `rtt`.
    - **Mapping Hardening**: 
        - `TelemetryProtobufMapper.kt`: Updated `mapToRealtime` and `mapProtoToSnapshot` to include raw sensor indices and RTT.
        - `TelemetryMapper.kt`: Fixed field omissions in all Snapshots and status mappers. Added RTT to tick outputs.
        - `TelemetryMerger.kt`: Ensured forensic data survives coordinate-stale packets.
    - **Relay Mechanism**:
        - `relay-server/index.js`: Implemented `violation_event` routing.
        - `ConnectivitySuite.kt`: Added `emitViolation` and `violation_relay` handler to mirror remote violations in the local `MainRepository`.
        - `ServiceForensicUseCase.kt`: Proactively emits violations via `ConnectivitySuite` when markers are latched.
- **Outcome**: 🟢 **PASSED**. (SOT ID 813).

## ISSUE-OCT22.0-F3: Connection State Asymmetry & Telemetry Leak (Oct22.12)
- **Resolved**: Oct22.12
- **Description**: When IDs were mismatched (e.g., V2 on Viewer vs V on Tracker), the Viewer erroneously showed a GREEN "TRK" badge and active telemetry despite no authenticated connection.
- **Remediation**:
    - **SignalingValidator Hardening**: Refactored `shouldProcessLocationUpdate` to strictly enforce `viewer_id` symmetry for both Tracker and Viewer roles. (R-ID 616).
    - **Cross-Viewer Blocking**: Implemented logic in `SignalingValidator` to explicitly reject all Viewer-to-Viewer traffic in Viewer mode.
- **Outcome**: 🟢 **PASSED**. (SOT ID 812).

## ISSUE-OCT22.0-F2: Setup Exit Authority & Lifecycle Failure (Oct22.10)
- **Resolved**: Oct22.10
- **Description**: The Setup overlay could not be closed via UI; the system Back button caused the entire application to exit.
- **Remediation**:
    - **BackHandler Integration**: Implemented `BackHandler` in `SettingsOverlay` and `PhoneSetupOverlay`.
    - **Exit Authority**: Added Close (X) icon to `PhoneSetupOverlay`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 811).

## ISSUE-OCT22.0-F1: Setup Configuration Failure (Oct22.0)
- **Resolved**: Oct22.0
- **Description**: ID persistence was blocked in `SettingsUseCase`.
- **Remediation**: Corrected logic block in `SettingsUseCase` to ensure draft settings are committed to DataStore. Added LoadDefaults.
- **Outcome**: 🟢 **PASSED**. (SOT ID 810).
