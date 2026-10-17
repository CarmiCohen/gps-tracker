# Resolution Archive

## SOT ID 754: JNI Memory Pressure Parity (Oct17.2)
- **Resolved**: Oct17.2
- **Description**: Hardened the JNI pipeline to ensure consistent memory pressure throttling across all sensor paths. Previously, only Vibration respected native pressure flags.
- **Outcome**: 
    - Resolved ISSUE-OCT16.11-F2 (JNI Memory Pressure Throttling Parity).
    - Updated `AcousticBatch` and `ProximityBatch` JNI outputs to include `isMemoryPressureThrottled`.
    - Integrated throttled state into `HardwareSuite` forensic snapshots.
    - Verified cross-sensor load shedding behavior.
    - Synchronized documentation to `Oct17.2`.

## SOT ID 753: JNI Telemetry Autonomy Hardening (Oct17.2)
- **Resolved**: Oct17.2
- **Description**: Migrated telemetry evaluation logic from Kotlin placeholders to C++ implementations to ensure consistency and performance. Addressed missing logic for Jamming candidacy, Muzzle resets, and Acoustic spike detection in the native layer.
- **Outcome**:
    - Resolved ISSUE-OCT16.11-F1 (JNI Telemetry Integration Hardening).
    - Completed `n19` (Vibration) implementation with `isJammingCandidate`, `muzzleResetTriggered`, and `isMemoryPressureThrottled`.
    - Completed `n22` (Acoustic) implementation with full spike detection.
    - Updated `EngineConstants.h` with physics-parity thresholds.
    - Added Architectural Rule R-ID 603 to `SOT_MASTER_REQUIREMENTS.md`.
    - Synchronized documentation to `Oct17.2`.

## SOT ID 752: Vibration NaN Hardening & JNI Alignment (Oct17.2)
- **Resolved**: Oct17.2
- **Description**: Fixed a regression where HUD vibration metrics displayed `NaNG`. Identified root causes as `NaN` poisoning in native filter logic and shared buffer misalignment on 64-bit boundaries.
- **Outcome**:
    - Resolved ISSUE-OCT16.9-F2 (Vibration Telemetry NaN).
    - Hardened `app/src/main/cpp/jdhardware-jni.cpp` with `std::isnan()` guards in `n19` and supporting math functions.
    - Re-aligned `sharedStateBuffer` in `JdHardwareManager.kt` to ensure all 8-byte primitives (Double/Long) sit on 8-byte offsets.
    - Updated JNI input/output offsets to match Kotlin alignment.
    - Added Architectural Rule R-ID 602 to `SOT_MASTER_REQUIREMENTS.md`.
    - Synchronized documentation to `Oct17.2`.

... [Previous entries preserved] ...
