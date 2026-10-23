# Project History & Versioning (Oct24.00)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

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
