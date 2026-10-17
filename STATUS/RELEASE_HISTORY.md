# Project History & Versioning (Oct17.11)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct17.11 (Full System Audit & Hardening)
- **Full System Audit**: Successfully executed 43 unit tests and 27 instrumentation tests on A15 hardware.
- **Instrumentation Test Hardening (#TEST-OCT17.11-T1)**: Resolved race conditions in `ForensicStressAuditTest` by inhibiting the background drainer during capacity audit.
- **Build Dependency Alignment**: Corrected `compose.bom` platform alignment for `androidTest` to resolve build failures.
- **Version Synchronization**: Synchronized all UI and test expectations to version `Oct17.11`.

## Oct16.10 (Vibration NaN Hardening)
- **Vibration NaN Hardening (#ISSUE-OCT16.9-F2)**: Resolved HUD `NaNG` regression by re-aligning `sharedStateBuffer` to strict 8-byte boundaries and implementing `isnan()` guards in `jdhardware-jni.cpp`. (R-ID 602).
- **Consolidated JNI Offsets**: Standardized buffer offsets across Kotlin and C++ to prevent data corruption in cross-layer telemetry.

... (Historical records continue below)
