# Project History & Versioning (Oct24.2)

**For historical records (v9.3.0 and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct24.2 (Signaling: Offline Violation Sync)
- **ISSUE-OCT23.101-F6 Remediation**: Resolved offline violation sync gaps.
- **Persistence Layer**: Implemented `PendingViolationEntity` and Room Migration 87->88 to buffer unsent violation events.
- **Unified Actor**: Extended `OfflineRepository` with a `PersistenceTask` actor to sequentialize status and violation storage, preventing database contention.
- **Automatic Recovery**: Hardened `ConnectivitySuite` to buffer events during offline states and automatically flush them upon network restoration.
- **Version Alignment (#ISSUE-OCT24.00-F5)**: Synchronized all forensic metadata and unit test expectations to version `Oct24.2`.

## Oct23.121 (Forensic: Violation Integrity)
- **ISSUE-OCT23.101-F4 Remediation**: Finalized violation persistence integrity.
- **Flag Propagation**: Synchronized `isJammingCandidate` through `AppAlarmManager`, `ServiceForensicUseCase`, `ConnectivitySuite` (signaling), and `MainRepository` (persistence).
- **Model Hardening**: Updated `ViolationPoint` UI model and `ViolationEntity` to preserve jammer discrimination metadata in historical views.

## Oct23.120 (Forensic: Hysteresis Parity)
- **ISSUE-OCT23.101-F3 Remediation**: Resolved missing stationary duration propagation.
- **Schema Update**: Added `stationary_duration_ms` to `RealtimeStatus` and `TrackerStatusProto`.
- **Persistence Parity**: Extended Room entities (`LogEntity`, `HistoryEntity`, `PendingStatusEntity`) with `stationaryDurationMs` and implemented Database Migration 86->87.
- **Engine Sync**: Ensured `stationaryDurationMs` is preserved in `SystemHealthState` and `EngineConnectionPoint` to maintain adaptive duty-cycle consistency across peer updates.

## Oct23.109 (Signaling: Concurrency Hardening)
- **ISSUE-OCT23.102-F9 Remediation**: Eliminated shared member flyweights in `ConnectivitySuite.kt`.
- **Thread Safety**: Migrated local status and coordinate reconstruction state to atomic primitives (`AtomicReference`, `AtomicInteger`) to prevent race conditions during concurrent signaling events.

## Oct23.108 (Signaling: Protobuf Integrity)
- **ISSUE-OCT23.102-F8 Remediation**: Resolved Protobuf default value pollution.
- **Schema Hardening**: Applied `optional` keyword to scalar fields in `app_settings.proto`.
- **Mapping Safety**: Hardened `TelemetryMapper.kt` with `hasField()` checks to prevent omitted fields from overwriting valid state with defaults.

## Oct23.107 (Signaling: Delta Drift Prevention)
- **ISSUE-OCT23.102-F7 Remediation**: Implemented periodic absolute coordinate refresh in `TelemetryProtobufMapper`.
- **Drift Control**: Forced absolute coordinate transmission every 50 packets to prevent permanent spatial drift in delta-compressed streams.

## Oct23.106 (Signaling: JSON State Preservation)
- **ISSUE-OCT23.101-F2 Remediation**: Fixed JSON mapping state overwrites in `TelemetryMapper.kt`.
- **Fallback Logic**: Implemented current-state fallback for optional JSON keys (proximity, rtt, snapshots) to prevent forensic regressions.

## Oct23.105 (Engine: Merge Logic Expansion)
- **ISSUE-OCT23.101-F1 Remediation**: Expanded `TelemetryMerger.kt` logic to preserve integrity flags (`isJammer`, `isStalled`) and SIT metrics during GPS outages.

## Oct23.101 (Audit: Telemetry Sync Recheck)
- **ISSUE-OCT22.0-F4 Audit**: Conducted a rigorous recheck of Telemetry Propagation & Violation Relay implementation.
- **Audit Failure**: Identified 6 residual gaps and regressions (ISSUE-OCT23.101-F1 to F6).

## Oct23.100 (UI UX Simplification)
- **BackHandler Composition Local (#SIMP-OCT22.10-S1)**: Consolidated back button interception into a reusable `LocalBackAction` Composition Local.

## Oct24.00 (Build Stabilization & Infrastructure Hardening)
- **Build Infrastructure Hardening (#ISSUE-OCT24.00-F1)**: Upgraded toolchain to Kotlin `1.9.24` and JVM `17`.
- **Signaling Discovery Logic (#ISSUE-OCT24.0-F2)**: Relaxed `isDiscoveryMode` to allow pairing with default Trackers.
- **Security Hardening (#ISSUE-OCT24.00-F3)**: Enforced `RECEIVER_NOT_EXPORTED` flags for internal events.
