# GPS-Tracker (Pure Native APK)

**Version: 8.9.5**

This project is a **Pure Native Android Application** built with Kotlin and Jetpack Compose, designed for high-precision background tracking and remote monitoring in demanding environments.

## Recent Changes (v8.9.5)
- **Power Forensic Parity (Issue 192)**: Achieved absolute forensic parity for battery current (`currentMa`). currentMa is now persisted in Database v35 and TrackerStatusProto (AppSettings) to ensure remote power-deficit alerts and ribbons persist across restarts and backfill.
- **Viewer Background Location (Issue 189)**: Implemented 10s background polling for Viewer-side location. Updated LocationProcessor to calculate relative distance between nodes, ensuring geofence alerts and "Distance to Tracker" updates persist when the app is in the background.
- **evaluateAlarms Parameter Sync**: Hardened the telemetry pipeline by ensuring all forensic fields (including trackerCurrentMa) are correctly passed to the engine for remote alarm evaluation.

## Recent Changes (v8.9.2)
- **Branding Finalization (Issue 182/R935)**: Standardized to John Deere Green `#367C2B` and established `jd_app_icon.xml` as the primary asset. Redundant legacy branding assets marked for removal.
- **Forensic Parity (Issue 178/179)**: Achieved full field parity for `verticalVelocity` and SIT metrics (`sitBaro`, `sitTilt`, `sitShock`) across the telemetry pipeline.
- **Viewer Service Completion (Issue 185)**: Implemented local log and trail persistence in `ViewerService` to ensure forensic mirroring between roles.
- **GPS Stability Audit (Issue 168)**: Integrated 10Hz polling verification suite to monitor fix reliability on high-frequency devices (Xiaomi/Samsung).

## Recent Changes (v8.8.35)
- **Database Schema Cleanup (Issue 159)**: Formally removed legacy `ver` and `vid` columns from SQLite tables via Room Migration v33.

## Project Core Features
- **Pure Logic Engine (:core:engine)**: Physically isolated tracking math, free from Android framework dependencies.
- **Zero-Lag Filtering**: Multi-layered sentinel (ImmFilter + Behavioral) for real-time jump rejection.
- **Physical Tamper Detection**: Acoustic fast-path, light-jump detection, and 3D orientation monitoring.
- **Anti-Stall Logic**: Monotonic clock regression guards and GPS stall detection.
- **Ribbon History**: Multi-resolution history tracking with worst-case aggregation (4M to 7D).

## Technical Specifications
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin 2.0+ / Compose 1.7+
- **Architecture**: Clean Architecture with UseCases and Dagger Hilt
- **Map Engine**: osmdroid with persistent marker pooling
