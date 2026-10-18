# Project History & Versioning (Oct18.2)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct18.2 (Test Mode DI Hardening)
- **Test Mode Dependency Injection (#SIMP-OCT17.11-S1)**: Eliminated static state leakage in `LogRepository` by migrating to a Hilt-injected `TestModeProvider`. This ensures total lifecycle isolation for background jobs during instrumentation tests.
- **Infrastructure Decoupling**: Created `InfrastructureModule` to separate environment-specific bindings from core application logic.
- **Test Suite Alignment**: Synchronized `ForensicStressAuditTest` and `GpsApplication` with the new DI-based test environment detection.

## Oct18.0 (JNI Alignment & NaN Hardening)
- **Vibration NaN Hardening (#ISSUE-OCT16.9-F2)**: Resolved HUD `NaNG` regression by re-aligning `sharedStateBuffer` to strict 8-byte boundaries and implementing `isnan()` guards in `jdhardware-jni.cpp`. (R-ID 602/761).
- **Consolidated JNI Offsets**: Standardized buffer offsets across Kotlin and C++ to prevent data corruption in cross-layer telemetry.

... (Historical records continue below)
