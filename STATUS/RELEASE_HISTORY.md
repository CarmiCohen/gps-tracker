# Project History & Versioning (July.23.11)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## July.23.11 (Stealth & Startup Hardening)
- **Tracker Stealth Violation (R872)**: Hardened `AppAlarmManager.kt` to suppress `shouldPlaySiren` in tracker mode. Trackers now remain strictly silent even during system violations, preventing accidental detection.
- **FGS Startup Stabilization (R406b)**: Moved `startServiceForeground()` to the Main-thread `onCreate` in `BaseMonitorService.kt`. This fixes the `ForegroundServiceDidNotStartInTimeException` that caused crash loops during automatic restoration from the landing page.

## July.23.10 (Hardware Hardening & Status Consistency)
- **SRV Status Consistency (#533)**: Modified `CommunicationManager.kt` to proactively update `TelemetryRepository` on socket connection state changes (Connect, Disconnect, Reconnect, Error). This ensures the UI "SRV" badge reflects real-time status immediately.
- **Step Detector Hardening (#098)**: Implemented explicit `ACTIVITY_RECOGNITION` permission check in `AppSensorManager.kt` before registering the Step Detector on Android 10+. This prevents hardware-level `fail(2)` (Permission Denied) errors.

... [See historical logs for full records]
