# Resolution Archive

## SOT ID 752: Vibration NaN Hardening & JNI Alignment (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: Fixed a regression where HUD vibration metrics displayed `NaNG`. Identified root causes as `NaN` poisoning in native filter logic and shared buffer misalignment on 64-bit boundaries.
- **Outcome**:
    - Resolved ISSUE-OCT16.9-F2 (Vibration Telemetry NaN).
    - Hardened `app/src/main/cpp/jdhardware-jni.cpp` with `std::isnan()` guards in `n19` and supporting math functions.
    - Re-aligned `sharedStateBuffer` in `JdHardwareManager.kt` to ensure all 8-byte primitives (Double/Long) sit on 8-byte offsets.
    - Updated JNI input/output offsets to match Kotlin alignment.
    - Added Architectural Rule R-ID 602 to `SOT_MASTER_REQUIREMENTS.md`.
    - Incremented version to `Oct14.1`.

## SOT ID 751: Project-Wide Test Cycle & Alarm Authority Fix (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: Executed full unit, hardening, and stress test suites. Identified and resolved a regression in `MainAlarmLogic` that prevented Viewer devices from triggering sirens for remote tracker violations.
- **Outcome**: 
    - Resolved ISSUE-OCT16.9-F1 (Viewer Remote Alarm Authority).
    - Verified all 47 engine unit tests and 25 app instrumented tests.
    - Updated `test_results.md` and `SOT_MASTER_REQUIREMENTS.md` with rule R-ID 589.
    - Incremented version to `Oct14.1`.

... [Previous entries preserved] ...
