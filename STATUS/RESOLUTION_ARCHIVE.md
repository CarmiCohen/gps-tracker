# Resolution Archive - GPS Tracker (Oct24.16)

## SIMP-OCT22.0-S1: Settings Validation Centralization (Oct24.16)
- **Resolved**: Oct24.16
- **Description**: Identity uniqueness and regex validation logic was coupled with the UI and Repository layers. This task centralized all validation rules into a pure engine-level `ConfigValidator`.
- **Remediation**:
    - Created `ConfigValidator` in `:core:engine` to serve as the single source of truth for configuration rules.
    - Delegated validation methods in `SignalingConstants` to the new validator.
    - Updated `SettingsRepository.commitDraftSettings` to utilize `ConfigValidator.validateIdentityUniqueness` for atomic validation during commit.
    - Standardized error messages (e.g., reserved ID conflicts) within the validator.
    - Added comprehensive unit tests in `ConfigValidatorTest`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 830).

## ISSUE-OCT24.14-F1: Version Alignment Failure Remediation (Oct24.14)
- **Resolved**: Oct24.14
- **Description**: `MainViewModelMappingTest` failed because it was hardcoded to expect "Oct24.2" or "Oct24.12", causing failures as the project versioned to "Oct24.14".
- **Remediation**:
    - Synchronized `app/build.gradle` `versionName` to `Oct24.14`.
    - Updated `MainViewModelMappingTest.kt` to expect `Oct24.14` in `Verify Dashboard Connectivity Watchdog`.
- **Outcome**: 🟢 **PASSED**.

## ISSUE-OCT24.10-F1: Native SDK Initialization Failure (Oct24.12)
- **Resolved**: Oct24.12
- **Description**: `NativeThrottlingStressTest` failed because `libjdHardware.so` was not packaged in the APK, leading to `JdHardwareManager.initialize` returning false.
- **Remediation**:
    - Added `externalNativeBuild` configuration to `app/build.gradle` to integrate CMake build process.
    - Specified C++17 and the path to `src/main/cpp/CMakeLists.txt`.
    - Verified that `libjdHardware.so` is correctly built, linked, and loaded during instrumentation tests.
- **Outcome**: 🟢 **PASSED**. (SOT ID 825).

## ISSUE-OCT23.101-F6: Offline Violation Event Sync Remediation (Oct24.2)
- **Resolved**: Oct24.2
- **Description**: Discrete violation markers (emitted via `violation_event`) were not buffered for offline synchronization, resulting in data loss during network outages.
- **Remediation**:
    - Created `PendingViolationEntity` and `PendingViolationDao` in `Database.kt` for persistent storage of unsent violation events.
    - Implemented `MIGRATION_87_88` and bumped Room version to `88`.
    - Extended `OfflineRepository` with a `PersistenceTask` sealed class and a unified persistence actor to handle both status updates and violation events sequentially.
    - Updated `ConnectivitySuite.emitViolation` to detect offline states and buffer events into `OfflineRepository`.
    - Hardened `ConnectivitySuite.flushPendingUpdates` to automatically synchronize buffered violations upon network recovery.
- **Outcome**: 🟢 **PASSED**. (SOT ID 824).

## ISSUE-OCT24.00-F5: Version Alignment Failure Remediation (Oct24.2)
- **Resolved**: Oct24.2
- **Description**: `MainViewModelMappingTest` failed due to version mismatch between `BuildConfig.VERSION_NAME` (`Oct23.121`) and test expectations (`Oct24.00`).
- **Remediation**:
    - Synchronized `app/build.gradle` `versionName` to `Oct24.2`.
    - Updated `MainViewModelMappingTest.kt` and `DashboardMapper.kt` documentation to align with `Oct24.2`.
- **Outcome**: 🟢 **PASSED**.

## ISSUE-OCT23.101-F4: Violation Persistence Integrity Remediation (Oct23.121)
- **Resolved**: Oct23.121
- **Description**: `isJammingCandidate` was missing from violation reporting and persistence paths.
- **Remediation**:
    - Updated `ViolationPoint` UI model and `ViolationEntity` to include `isJammingCandidate`.
    - Hardened `MainRepository.addViolation` and its mapping logic.
    - Updated `ConnectivitySuite.emitViolation` and `violation_relay` signaling handler.
- **Outcome**: 🟢 **PASSED**. (SOT ID 823).

## ISSUE-OCT23.101-F3: Stationary Duration Propagation Remediation (Oct23.120)
- **Resolved**: Oct23.120
- **Description**: `stationary_duration_ms` was ignored in deserialization and persistence paths.
- **Remediation**:
    - Updated `app_settings.proto` schema.
    - Extended `LogEntity`, `HistoryEntity`, and `PendingStatusEntity` in `Database.kt`.
    - Implemented `MIGRATION_86_87` (Room v87).
    - Hardened `TelemetryMapper.kt` and `TelemetryProtobufMapper.kt`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 822).

## ISSUE-OCT23.102-F9: Telemetry Flyweight Race Condition (Oct23.109)
- **Resolved**: Oct23.109
- **Description**: Shared flyweights in `ConnectivitySuite.kt` caused data corruption.
- **Remediation**: Replaced shared flyweights with local instances and atomic references.
- **Outcome**: 🟢 **PASSED**. (SOT ID 821).

## ISSUE-OCT23.102-F8: Protobuf Default Value Pollution (Oct23.108)
- **Resolved**: Oct23.108
- **Remediation**: Applied `optional` keyword in Proto schema and added `hasField()` checks in `TelemetryMapper.kt`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 820).

## ISSUE-OCT23.102-F7: Delta Compression Drift Prevention (Oct23.107)
- **Resolved**: Oct23.107
- **Remediation**: Implemented periodic absolute coordinate refresh in `TelemetryProtobufMapper`.
- **Outcome**: 🟢 **PASSED**. (SOT ID 819).

## ISSUE-OCT23.101-F2: JSON Mapping State Overwrite (Oct23.106)
- **Resolved**: Oct23.106
- **Remediation**: Hardened `TelemetryMapper.kt` to use cached values as fallback for optional fields.
- **Outcome**: 🟢 **PASSED**. (SOT ID 818).

## ISSUE-OCT23.101-F1: Incomplete Telemetry Merge Logic (Oct23.105)
- **Resolved**: Oct23.105
- **Remediation**: Expanded `preferCached` block in `TelemetryMerger.kt` to include critical integrity flags.
- **Outcome**: 🟢 **PASSED**. (SOT ID 813).

## SIMP-OCT22.10-S1: BackHandler Composition Local Consolidation (Oct23.100)
- **Resolved**: Oct23.100
- **Remediation**: Implemented `LocalBackAction` for unified back navigation.
- **Outcome**: 🟢 **PASSED**. (SOT ID 817).

## ISSUE-OCT24.00-F1/F3/F4: Build Stabilization & Security Hardening (Oct24.00)
- **Resolved**: Oct24.00
- **Outcome**: 🟢 **PASSED**. (SOT ID 816).

## ISSUE-OCT24.00-F2: Signaling Discovery Logic Relaxation (Oct24.00)
- **Resolved**: Oct24.00
- **Outcome**: 🟢 **PASSED**. (SOT ID 815).

## ISSUE-OCT22.16-F1/F2/F3: Forensic Trace Raw Sensor Expansion (Oct23.00)
- **Resolved**: Oct23.00
- **Outcome**: 🟢 **PASSED**. (SOT ID 814).
