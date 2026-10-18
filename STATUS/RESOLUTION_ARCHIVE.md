# Resolution Archive

## SIMP-OCT17.11-S1: Test Mode Dependency Injection (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: `LogRepository` relied on a static `AtomicBoolean` for test mode, which caused background job leakage and race conditions between instrumentation tests.
- **Remediation**:
    - Introduced `TestModeProvider` interface and `ProductionTestModeProvider`.
    - Created `InfrastructureModule` for decoupled environment bindings.
    - Implemented `TestModeModule` in `androidTest` to inject `ManualTestModeProvider` for all instrumented tests.
    - Refactored `LogRepository` to use injected provider, eliminating static side effects.
    - Cleaned up `GpsApplication` and `ForensicStressAuditTest` legacy static calls.
- **Outcome**: 🟢 **PASSED**. Total test isolation achieved. Removed potential for background drainer interference in stability audits. (SOT ID 762).

## ISSUE-OCT16.9-F2: JNI Buffer NaNG Regression Hardening (Oct14.1)
- **Resolved**: Oct14.1
- **Description**: HUD "Vibration" fields displayed `NaNG` during extreme I/O pressure on A15 hardware due to JNI buffer misalignment and missing `NaN` sanitization.
- **Remediation**:
    - Re-aligned `GnssHealthBatch` in `JdHardwareManager.kt` to strict 8-byte boundaries.
    - Migrated `n21` (GNSS) in `jdhardware-jni.cpp` to use `std::memcpy` for all buffer accesses.
    - Added `isnan()` sanitization for `snr`, `thermal`, and `heap` inputs in `n19` (Vibration).
    - Incremented system version to `Oct14.1`.
- **Outcome**: 🟢 **PASSED**. Corrected alignment prevents data corruption on ARMv8 SoCs. `NaN` reinforcement ensures HUD stability under pressure. (SOT ID 761).

... [Remaining entries preserved] ...
