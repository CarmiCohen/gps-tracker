# Resolution Archive

## SIMP-OCT15.6-S1: Automated Version Synchronization (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: Centralized the version name to prevent desynchronization between the build system, UI, and Engine.
- **Remediation**:
    - Moved `versionName` to `gradle.properties` as `APP_VERSION_NAME`.
    - Configured `app/build.gradle` to use this property for `versionName` and ensured `buildConfig true` was active.
    - Refactored `MainAppContent.kt` and `DashboardMapper.kt` to dynamically reference `BuildConfig.VERSION_NAME`.
- **Outcome**: Eliminated manual string updates for versioning, ensuring a single source of truth. (SOT ID 757).

## ISSUE-OCT15.6-T1: Version Name Verification (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: Added formal version verification to the manual test procedure to ensure UI and Engine strings are synchronized with the build configuration.
- **Remediation**: 
    - Updated `TESTS/TEST_PROCEDURE.md` to include Step 1.3: Version Verification.
    - Synchronized `app/build.gradle` (`versionName`), `MainAppContent.kt` (UI Label), and `DashboardMapper.kt` (Engine string) to `Oct14.1`.
- **Outcome**: Established a hardening baseline for release integrity. (SOT ID 756).

## SOT ID 734: JNI Alignment Hardening (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: Fixed a critical regression where Dashboard telemetry fields (Vibration, Rolling Vibe) displayed `NaNG` on Samsung A15.
- **Root Cause**: Memory alignment violations on the MediaTek Helio G99 SoC when using direct pointer casting `*(double*)(ptr + offset)` on `ByteBuffer` addresses.
- **Remediation**:
    - Hardened `app/src/main/cpp/jdhardware-jni.cpp` by replacing direct pointer casting with `std::memcpy` for all 8-byte primitives (double, int64_t).
    - Affected methods: `n19` (Vibration), `n21` (GNSS), `n22` (Acoustic), `n23` (Proximity), and `n24` (Pressure).
- **Outcome**: Verified numeric telemetry pulse and stability on physical A15 hardware.

## DOC-OCT17.4: Version Synchronization (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: UI version label was stuck at `Oct14.1_PLATINUM` despite `app/build.gradle` being at `Oct14.1`.
- **Remediation**: Updated hardcoded label in `MainAppContent.kt` (line 116) to `"Oct14.1_PLATINUM"`.
- **Outcome**: UI and build system are now synchronized.

## SOT ID 755: Project-Wide Test Cycle (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: Conducted a full project test cycle including 43 app unit tests, engine unit tests, and 27 connected instrumented tests on the A15 device.
- **Outcome**:
    - Verified 100% pass rate for automated test suite.
    - Confirmed stability on physical A15 hardware (R58X40GV2AR).
    - Incremented version to Oct14.1.
    - Synchronized all status documentation.

... [Previous entries preserved] ...
