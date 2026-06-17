# GPS-Tracker (Pure Native APK)

**Version: 8.8.35**

This project is a **Pure Native Android Application** built with Kotlin and Jetpack Compose, designed for high-precision background tracking and remote monitoring in demanding environments.

## Recent Changes (v8.8.35)
- **Database Schema Cleanup (Issue 159)**: Formally removed legacy `ver` and `vid` columns from SQLite tables via Room Migration v33. Schema is now clean and aligned with the simplified forensic model.
- **Global Version Synchronization (Issue 156)**: Synchronized all documentation (SOT, Alarms, Sentinel) and build scripts to the v8.8.35 baseline.

## Recent Changes (v8.8.34)
- **Forensic Simplification**: Removed redundant version fields from all active data models and telemetry pipelines.
- **Build Stability (Issue 155)**: Resolved model synchronization conflicts following forensic field removal.

## Recent Changes (v8.8.32)
- **Forensic Parity Fix (Issue 149)**: Achieved symbol parity for forensic markers (Magenta Squares for Jumps, Red Circles for Out-of-Range).
- **Viewer Jump Latching**: Viewers now explicitly record Tracker-calculated visual jumps to local forensics.

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
