# Resolution Archive - GPS Tracker (Oct23.120)

## ISSUE-OCT23.101-F3: Stationary Duration Propagation Remediation (Oct23.120)
- **Resolved**: Oct23.120
- **Description**: `stationary_duration_ms` was serialized in `toMap()` but ignored in all deserialization and persistence paths, causing duty-cycle logic (e.g., adaptive acoustic intervals) to fail after peer updates or offline restoration.
- **Remediation**:
    - Updated `app_settings.proto` to include `stationary_duration_ms` in `RealtimeStatus` and `TrackerStatusProto`.
    - Extended `LogEntity`, `HistoryEntity`, and `PendingStatusEntity` in `Database.kt` with `stationaryDurationMs`.
    - Incremented Room database version to `87` and implemented `MIGRATION_86_87`.
    - Hardened `TelemetryMapper.kt` and `TelemetryProtobufMapper.kt` to synchronize `stationaryDurationMs` across all signaling (JSON/Proto), persistence, and engine mapping paths.
    - Synchronized `EngineConnectionPoint` and `SystemHealthState` in `:core:engine` to preserve hysteresis state during evaluations.
- **Outcome**: 🟢 **PASSED**. (SOT ID 822).

## ISSUE-OCT23.102-F9: Telemetry Flyweight Race Condition (Oct23.109)
- **Resolved**: Oct23.109
- **Description**: `ConnectivitySuite.kt` used shared member flyweights (`snapshotFlyweight`, `statusFlyweight`, etc.) across multiple asynchronous processing paths, leading to data corruption and inconsistent state when multiple signaling updates arrived concurrently.
- **Remediation**:
    - Replaced shared `LocationUpdate` flyweights with local instances within `processBinaryUpdateInternal`, `processJsonUpdateInternal`, and `flushPendingUpdates`.
    - Migrated `localStatusFlyweight` to a thread-safe `AtomicReference<LocationUpdate>` named `localStatus`.
    - Converted coordinate reconstruction fields (`remoteLastLatE7`, `remoteLastLngE7`) to `AtomicInteger` to ensure thread-safe delta calculations.
    - Updated `updateLocalTelemetry` to use `AtomicReference.set()` with a deep copy (`duplicate()`) of the incoming status.
- **Outcome**: 🟢 **PASSED**. (SOT ID 821).

## ISSUE-OCT23.102-F8: Protobuf Default Value Pollution (Oct23.108)
- **Resolved**: Oct23.108
- **Description**: Many scalar telemetry fields in `RealtimeStatus` and `TrackerStatusProto` lacked the `optional` keyword, causing `TelemetryMapper` to overwrite valid local state with Protobuf defaults (0, false) when fields were omitted by the sender.
- **Remediation**: 
    - Applied `optional` keyword to all scalar fields in `app_settings.proto` (RealtimeStatus, TrackerStatusProto, LogicStateProto, AlertSettingsProto, ActiveAlarmProto).
    - Hardened `TelemetryMapper.kt` mapping methods (`mapProtoToStatus`, `mapProtoToSnapshot`, `mapProtoToApp`) with explicit `hasField()` checks to prevent default value pollution.
    - Synchronized `SOT_MASTER_REQUIREMENTS.md` with new architectural rule (R-ID 626).
- **Outcome**: 🟢 **PASSED**. (SOT ID 820).

## ISSUE-OCT23.102-F7: Delta Compression Drift Prevention (Oct23.107)
- **Resolved**: Oct23.107
- **Description**: `TelemetryProtobufMapper` lacked a periodic absolute coordinate refresh, risking permanent drift if delta packets were lost.
- **Remediation**: Implemented `DELTA_REFRESH_INTERVAL` (50 packets) in `SignalingConstants.kt` and updated `TelemetryProtobufMapper.mapToRealtime` to force absolute coordinates when the `packetCounter` in `SignalingDeltaState` reaches the interval.
- **Outcome**: 🟢 **PASSED**. (SOT ID 819).

## ISSUE-OCT23.101-F2: JSON Mapping State Overwrite (Oct23.106)
- **Resolved**: Oct23.106
- **Description**: `TelemetryMapper.kt` overwrote multiple fields with hardcoded defaults (e.g., `-1.0` for `proximityCm`, `0` for `rtt`) in `mapJsonToStatus` instead of preserving current cached values when keys are missing in JSON.
- **Remediation**: Hardened `mapJsonToStatus` and `mapJsonToSnapshot` to use the `current` state as fallback for all optional telemetry fields (proximity, rtt, snapshots, battery flags, etc.).
- **Outcome**: 🟢 **PASSED**. (SOT ID 818).

## ISSUE-OCT23.101-F1: Incomplete Telemetry Merge Logic (Oct23.105)
- **Resolved**: Oct23.105
- **Description**: `TelemetryMerger.kt` failed to merge critical integrity flags and SIT metrics when GPS was stale.
- **Remediation**: Expanded `preferCached` block in `TelemetryMerger.kt` to include `isJammer`, `isStalled`, SIT metrics, raw sensor values, and proximity/index metadata.
- **Outcome**: 🟢 **PASSED**. (SOT ID 813).

## ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Hardening (Oct22.16 / Oct23.101)
- **Resolved**: Oct22.16 (Initial) / **RE-AUDIT FAILED**: Oct23.101
- **Description**: Critical failure where forensic sensor data, RTT, and violation metrics were not synchronized between Tracker and Viewer.
- **Remediation History**:
    - **Oct22.16**: Synchronized RTT, Violation Percentage, and Jamming Candidate across Protobuf, JSON, and Database (v85).
    - **Oct23.101 Audit**: Identified 6 residual gaps including incomplete merge logic in `TelemetryMerger` (omitting `isJammer`/`isStalled` when GPS is stale), JSON mapping regressions (hardcoded defaults), and missing `stationary_duration_ms` propagation.
- **Outcome**: 🔴 **RE-AUDIT FAILED**. Residual hardening required (ISSUE-OCT23.101-F1 to F6). (SOT ID 813).

## SIMP-OCT22.10-S1: BackHandler Composition Local Consolidation (Oct23.100)
- **Resolved**: Oct23.100
- **Description**: Back button interception was fragmented across multiple overlays, leading to boilerplate and risk of inconsistent navigation behavior.
- **Remediation**: Implemented `LocalBackAction` and `BackPressInterceptor` to provide a unified mechanism for handling system back navigation.
- **Outcome**: 🟢 **PASSED**. (SOT ID 817).

## ISSUE-OCT24.00-F1/F3/F4: Build Stabilization & Security Hardening (Oct24.00)
- **Resolved**: Oct24.00
- **Remediation**: Fixed `activity-compose` artifact name, upgraded to JVM 17, and enforced broadcast receiver export security flags.
- **Outcome**: 🟢 **PASSED**. (SOT ID 816).

## ISSUE-OCT24.00-F2: Signaling Discovery Logic Relaxation (Oct24.00)
- **Resolved**: Oct24.00
- **Remediation**: Relaxed `isDiscoveryMode` to allow Trackers with default Viewer IDs to adopt custom viewers.
- **Outcome**: 🟢 **PASSED**. (SOT ID 815).

## ISSUE-OCT22.16-F1/F2/F3: Forensic Trace Raw Sensor Expansion (Oct23.00)
- **Resolved**: Oct23.00
- **Remediation**: Extended `ForensicSnapshot` and `LocationUpdate` with `lux` and `acoustic` fields for full raw sensor trace parity.
- **Outcome**: 🟢 **PASSED**. (SOT ID 814).
