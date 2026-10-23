# Resolution Archive - GPS Tracker (Oct22.16)

## ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Hardening (Oct22.16)
- **Resolved**: Oct22.16
- **Description**: Forensic sensor data (Noise/Lux/Vibe/Proximity) and Ping (RTT) values were not reaching the Viewer HUD (showing 0). Tracker violations were also not mirroring on the remote Viewer. Initial Oct22.13/Oct22.14 fixes had gaps in Protobuf sync, JSON key mismatches, and lacked Jammer Discrimination flag parity.
- **Remediation**:
    - **Protobuf Schema**: Expanded `RealtimeStatus` and `TrackerStatusProto` in `app_settings.proto` to include raw forensic fields, `rtt`, `violation_percentage`, and `is_jamming_candidate`.
    - **Mapping Hardening**: 
        - `TelemetryProtobufMapper.kt`: Updated `mapToRealtime` and `mapToPersistence` to include all forensic indices, RTT, proximity metrics, vibration floor, and jammer flags. Fixed missing `violationPercentage` and `isJammingCandidate` in `mapAppToPersistence`.
        - `TelemetryMapper.kt`: Fixed field omissions in all Snapshots and status mappers. Synchronized `violationPercentage` and `isJammingCandidate`. Hardened `mapStatusToPending` and `mapPendingToStatus`.
        - `TelemetryMerger.kt`: Ensured forensic data, session audit metrics, and jammer flags survive coordinate-stale packets.
        - `LocationUpdate.kt`: Fixed JSON key snake_case consistency for `adaptive_vibration_floor`, `is_cooling_mode_active`, `last_energy_duration_ms`, and `is_jamming_candidate`.
    - **Model Integrity**: Updated `ConnectionPoint` and `LogEntry` in `Models.kt` to include `isJammingCandidate` for history parity.
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
