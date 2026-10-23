# Project History & Versioning (Oct23.100)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct23.100 (UI UX Simplification)
- **BackHandler Composition Local (#SIMP-OCT22.10-S1)**: Consolidated back button interception into a reusable `LocalBackAction` Composition Local and `BackPressInterceptor` component.
- **Boilerplate Reduction**: Refactored all major overlays (Settings, Log, GNSS, Ribbons, Alarms) to use the centralized mechanism, ensuring consistent navigation and preventing unintended app exits.
- **Modifier Integration**: Introduced `Modifier.backPressHandler` for flexible interception on specific UI elements.

## Oct24.00 (Build Stabilization & Infrastructure Hardening)
- **Build Infrastructure Hardening (#ISSUE-OCT24.00-F1)**: Upgraded toolchain to Kotlin `1.9.24` and JVM `17`. Corrected `activity-compose` dependency name and added missing `lifecycle-service` and `preference-ktx`.
- **Signaling Discovery Logic (#ISSUE-OCT24.00-F2)**: Relaxed `isDiscoveryMode` in `SignalingValidator` to allow Trackers in default state to adopt custom viewers during initial pairing.
- **Security Hardening (#ISSUE-OCT24.00-F3)**: Migrated all broadcast receiver registrations to `ContextCompat.registerReceiver` with explicit `RECEIVER_NOT_EXPORTED` flags for internal events, satisfying Android 14 requirements.
- **Test Suite Alignment (#ISSUE-OCT24.00-F4)**: Synchronized `MainViewModelMappingTest` version expectations and fixed Hilt instrumentation test dependencies using `testFixtures`.
- **Full Build Verification**: Successfully executed clean builds for all variants (`debug`, `release`, `androidTest`) and passed the entire unit test suite.

## Oct23.00 (Forensic Trace Raw Sensor Expanded)
- **Forensic Trace Raw Sensor Parity (#ISSUE-OCT22.16-F1)**: Extended the forensic audit log with raw sensor snapshots (`lux`, `acoustic`) to enable high-fidelity environmental analysis during violations.
- **Model Expansion**: Added `lux` and `acoustic` fields to `ForensicSnapshot` and integrated them into `LocationUpdate`, `LogEntry`, and `ConnectionPoint`.
- **Protobuf & Sync (#ISSUE-OCT22.16-F2)**: Synchronized `lux_snapshot` and `acoustic_snapshot` fields across all signaling (Realtime) and persistence (DataStore) layers.
- **Database Parity (#ISSUE-OCT22.16-F3)**: Verified Migration 85->86 adding `luxSnapshot` and `acousticSnapshot` columns to logs and history tables.
- **Build Restoration**: Resolved build failures in `SystemHealthState` delegates by aligning with the expanded forensic model.

## Oct22.16 (Full Forensic Parity Hardened)
- **Remediation Hardening (#ISSUE-OCT22.0-F4)**: Finalized full stack parity for forensic telemetry. Synchronized RTT, Violation Percentage, and Jammer Candidate flags across all persistence and mapping layers.
- **Protobuf Parity**: Synchronized `violationPercentage` and `isJammingCandidate` in `TelemetryProtobufMapper.mapAppToPersistence` to ensure history binary parity.
- **Mapping Integrity**: Synchronized `rtt` and `violationPercentage` in all `TelemetryMapper` paths (Snapshot, Status, App, Entity) to resolve residuals from Oct22.15.
- **Database Alignment**: Confirmed `PendingStatusEntity`, `HistoryEntity`, and `ViolationEntity` parity in v85.

## Oct22.15 (Forensic Sync & Jammer Parity Hardened)
- **Remediation Hardening (#ISSUE-OCT22.0-F4)**: Corrected residual gaps in forensic synchronization. Ensured Jammer Discrimination flags and RTT persistence are synchronized across Protobuf, JSON, and internal models.
- **Protobuf Parity**: Added `is_jamming_candidate` to all status protos. Added `rtt` to persistence proto for connection quality auditing.
- **JSON Standardization**: Standardized all telemetry JSON keys to snake_case (`is_cooling_mode_active`, `last_energy_duration_ms`, `is_jamming_candidate`).
- **Database Schema (v85)**: Implemented migration to add `violationPercentage` and `isJammingCandidate` columns to logs and history tables. Incremented to version 85 to add `rtt` to `PendingStatusEntity`.
- **Mapping Integrity**: Hardened `TelemetryProtobufMapper` and `TelemetryMapper` for field parity including jammer candidates.

## Oct22.14 (Forensic Sync Hardening)
- **Remediation Recheck (#ISSUE-OCT22.0-F4)**: Hardened forensic field synchronization (Proximity, Vibration Floor, RTT) and ensured session audit parity (Violation Percentage) across Protobuf/JSON.
- **Protobuf Schema**: Expanded `RealtimeStatus` to include `violation_percentage`.
- **Mapping Integrity**: Fixed omissions in `TelemetryMapper` ingestion and resolved JSON key inconsistency in `LocationUpdate.toMap`.
- **Merge Reliability**: Hardened `TelemetryMerger.mergeInto` to ensure all diagnostic metadata survives stale coordinate merges.

## Oct22.13 (Telemetry Propagation Hardening)
- **Telemetry Propagation & Sync (#ISSUE-OCT22.0-F4)**: Corrected critical failure where forensic sensor data and RTT were not reaching the Viewer.
- **Protobuf Extension**: Added raw forensic fields (Lux, Acoustic, Vibration) to `RealtimeStatus` schema to ensure wire-level parity.
- **Violation Relay**: Implemented Tracker-to-Viewer violation propagation path in `CommunicationManager` and `relay-server`.
- **Mapping Integrity**: Hardened `TelemetryMapper` and `TelemetryMerger` to ensure sensor indices survive even when coordinates are rejected as stale.

## Oct22.12 (Signaling Identity Symmetry)
- **Signaling Identity Symmetry (#ISSUE-OCT22.0-F3)**: Enforced strict Viewer ID symmetry in `SignalingValidator` for all signaling events (pulses, pings, pongs, telemetry).
- **Leak Prevention**: Explicitly blocked cross-viewer traffic in Viewer mode to prevent erroneous activity indications from unauthorized peers (R-ID 616).
- **Communication Hardening**: Integrated validator into `CommunicationManager` relay handlers to ensure UI status badges only transition to Green for authenticated peers.

## Oct22.10 (Setup Lifecycle Hardening)
- **Setup Exit Authority (#ISSUE-OCT22.0-F2)**: Integrated `BackHandler` into `SettingsOverlay` and `PhoneSetupOverlay` to prevent unintended application termination during configuration (R-ID 614).
- **Exit Authority Parity**: Added explicit Close (X) icon to `PhoneSetupOverlay` to ensure consistent exit mechanisms across all setup flows (R-ID 615).

## Oct22.0 (Setup & Configuration Hardening)
- **Setup Configuration Fix (#ISSUE-OCT22.0-F1)**: Resolved ID persistence blocks in `SettingsUseCase` to ensure customized Tracker/Viewer IDs are properly committed to DataStore.
- **Factory Reset Authority**: Implemented `LoadDefaults` to allow users to recover from invalid configurations.
- **UI Visibility**: Promoted Relay URL visibility and contrast in the Settings UI.
