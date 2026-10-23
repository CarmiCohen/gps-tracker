# Project History & Versioning (Oct23.101)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct23.101 (Audit: Telemetry Sync Recheck)
- **ISSUE-OCT22.0-F4 Audit**: Conducted a rigorous recheck of Telemetry Propagation & Violation Relay implementation.
- **Audit Failure**: Identified 6 residual gaps and regressions (ISSUE-OCT23.101-F1 to F6).
- **Merge Logic Gaps**: Found that `TelemetryMerger` omits critical flags (`isJammer`, `isStalled`) during GPS outages.
- **JSON Mapping Regressions**: Found that `TelemetryMapper` incorrectly resets several forensic fields to defaults when keys are missing in partial updates.
- **Propagation Gaps**: Found that `stationary_duration_ms` and `isJammingCandidate` (in violations) are not fully propagated across all layers.

## Oct23.100 (UI UX Simplification)
- **BackHandler Composition Local (#SIMP-OCT22.10-S1)**: Consolidated back button interception into a reusable `LocalBackAction` Composition Local and `BackPressInterceptor` component.
- **Boilerplate Reduction**: Refactored all major overlays (Settings, Log, GNSS, Ribbons, Alarms) to use the centralized mechanism, ensuring consistent navigation and preventing unintended app exits.
- **Modifier Integration**: Introduced `Modifier.backPressHandler` for flexible interception on specific UI elements.

## Oct24.00 (Build Stabilization & Infrastructure Hardening)
- **Build Infrastructure Hardening (#ISSUE-OCT24.00-F1)**: Upgraded toolchain to Kotlin `1.9.24` and JVM `17`. Corrected `activity-compose` dependency name and added missing `lifecycle-service` and `preference-ktx`.
- **Signaling Discovery Logic (#ISSUE-OCT24.0-F2)**: Relaxed `isDiscoveryMode` in `SignalingValidator` to allow Trackers in default state to adopt custom viewers during initial pairing.
- **Security Hardening (#ISSUE-OCT24.00-F3)**: Migrated all broadcast receiver registrations to `ContextCompat.registerReceiver` with explicit `RECEIVER_NOT_EXPORTED` flags for internal events, satisfying Android 14 requirements.
- **Test Suite Alignment (#ISSUE-OCT24.00-F4)**: Synchronized `MainViewModelMappingTest` version expectations and fixed Hilt instrumentation test dependencies using `testFixtures`.

## Oct23.00 (Forensic Trace Raw Sensor Expanded)
- **Forensic Trace Raw Sensor Parity (#ISSUE-OCT22.16-F1)**: Extended the forensic audit log with raw sensor snapshots (`lux`, `acoustic`) to enable high-fidelity environmental analysis during violations.
- **Model Expansion**: Added `lux` and `acoustic` fields to `ForensicSnapshot` and integrated them into `LocationUpdate`, `LogEntry`, and `ConnectionPoint`.
- **Protobuf & Sync (#ISSUE-OCT22.16-F2)**: Synchronized `lux_snapshot` and `acoustic_snapshot` fields across all signaling (Realtime) and persistence (DataStore) layers.
- **Database Parity (#ISSUE-OCT22.16-F3)**: Verified Migration 85->86 adding `luxSnapshot` and `acousticSnapshot` columns to logs and history tables.

## Oct22.16 (Full Forensic Parity Hardened)
- **Remediation Hardening (#ISSUE-OCT22.0-F4)**: Finalized full stack parity for forensic telemetry. Synchronized RTT, Violation Percentage, and Jammer Candidate flags across all persistence and mapping layers.
- **Protobuf Parity**: Synchronized `violationPercentage` and `isJammingCandidate` in `TelemetryProtobufMapper.mapAppToPersistence` to ensure history binary parity.
- **Mapping Integrity**: Synchronized `rtt` and `violationPercentage` in all `TelemetryMapper` paths (Snapshot, Status, App, Entity) to resolve residuals from Oct22.15.
- **Database Alignment**: Confirmed `PendingStatusEntity`, `HistoryEntity`, and `ViolationEntity` parity in v85.
