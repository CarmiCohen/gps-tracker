# Project History & Versioning (Oct16.10)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct16.10 (Vibration NaN Hardening)
- **Vibration NaN Hardening (#ISSUE-OCT16.9-F2)**: Resolved HUD `NaNG` regression by re-aligning `sharedStateBuffer` to strict 8-byte boundaries and implementing `isnan()` guards in `jdhardware-jni.cpp`. (R-ID 602).
- **Consolidated JNI Offsets**: Standardized buffer offsets across Kotlin and C++ to prevent data corruption in cross-layer telemetry.

## Oct16.9 (Alarm Authority Restoration)
- **Remote Alarm Authority (#ISSUE-OCT16.9-F1)**: Restored Viewer ability to trigger sirens for remote Tracker violations (Power, Tamper, Tilt, Acoustic, Lift) by removing invalid `isTrackerMode` guards in `MainAlarmLogic`. (R-ID 589).
- **Test Cycle Oct16.9**: Successfully executed 47 engine and 43 app unit tests with 100% pass rate.

## Oct16.8 (Chapter 4 Audit - Viewer & Remote Sync)
- **HUD Visibility & Occlusion (#ISSUE-OCT16.8-F3)**: Decoupled `AppMapContainer` from overlay gates, ensuring the map remains visible behind UI surfaces in Portrait mode. (Ch 4.11).
- **Sentinel Z-Index Hardening (#ISSUE-OCT16.8-F4)**: Migrated `AlarmOverlay` to a full-screen `Dialog` via `DialogHost`, ensuring absolute priority over navigation overlays. (R-ID 582, Ch 4.8).
- **Role Transition Grace (#ISSUE-OCT16.8-F2)**: Implemented 30s hardware boot grace in `AppAlarmManager` and reset service start timers during role switches to prevent immediate false-positive alarms. (Ch 4.4).
- **Termination Dialog Fix (#ISSUE-OCT16.8-F1)**: Centralized session termination confirmation in `DialogHost` to prevent occlusion by active overlays.

... (Historical records continue below)
