# Resolution Archive - GPS Tracker (Oct23.00)

## ISSUE-OCT22.16-F1/F2/F3: Forensic Trace Raw Sensor Expansion (Oct23.00)
- **Resolved**: Oct23.00
- **Description**: Forensic audit logs lacked raw sensor snapshots (`lux`, `acoustic`) at the trace level, limiting post-mortem analysis of environmental conditions during violations. Build failures were also occurring due to unresolved delegates in `SystemHealthState`.
- **Remediation**:
    - **Model Expansion**: Extended `ForensicSnapshot` in `core:engine` with `lux` and `acoustic` nullable Double fields.
    - **Delegate Integration**: Added `luxSnapshot` and `acousticSnapshot` delegates to `LocationUpdate` (Engine) and `ConnectionPoint` (App) for seamless access.
    - **Mapping Synchronization**: Updated `TelemetryMapper` and `TelemetryProtobufMapper` to propagate these snapshots through all signaling (Realtime) and persistence (DataStore/Room) layers.
    - **Logic Continuity**: Hardened `TelemetryMerger` to ensure environmental snapshots survive coordinate-stale packets during GPS signal loss.
    - **Build Restoration**: Resolved build errors in `SystemHealthState` by aligning with the expanded `ForensicSnapshot` model.
- **Outcome**: 🟢 **PASSED**. (SOT ID 814).

## ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Hardening (Oct22.16)
- **Resolved**: Oct22.16
- **Description**: Forensic sensor data (Noise/Lux/Vibe/Proximity) and Ping (RTT) values were not reaching the Viewer HUD (showing 0). Tracker violations were also not mirroring on the remote Viewer.
- **Remediation**:
    - **Protobuf Schema**: Expanded `RealtimeStatus` and `TrackerStatusProto` in `app_settings.proto` to include raw forensic fields, `rtt`, `violation_percentage`, and `is_jamming_candidate`.
    - **Mapping Hardening**: Updated `TelemetryProtobufMapper.kt` and `TelemetryMapper.kt` for full parity.
- **Outcome**: 🟢 **PASSED**. (SOT ID 813).

## ISSUE-OCT22.0-F3: Connection State Asymmetry & Telemetry Leak (Oct22.12)
- **Resolved**: Oct22.12
- **Description**: ID mismatch allowed unauthenticated telemetry visibility on Viewer.
- **Remediation**: Enforced strict `viewer_id` symmetry in `SignalingValidator`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 812).

## ISSUE-OCT22.0-F2: Setup Exit Authority & Lifecycle Failure (Oct22.10)
- **Resolved**: Oct22.10
- **Remediation**: Implemented `BackHandler` and Exit authority for setup overlays.
- **Outcome**: 🟢 **PASSED**. (SOT ID 811).

## ISSUE-OCT22.0-F1: Setup Configuration Failure (Oct22.0)
- **Resolved**: Oct22.0
- **Remediation**: Fixed `SettingsUseCase` persistence block.
- **Outcome**: 🟢 **PASSED**. (SOT ID 810).
