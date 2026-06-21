# GPS-Tracker (Pure Native APK)

**Version: 8.9.10**

This project is a **Pure Native Android Application** built with Kotlin and Jetpack Compose, designed for high-precision background tracking and remote monitoring in demanding environments.

## Recent Changes (v8.9.10)
- **Log Spatial Anchor (Issue #208)**: Implemented coordinate-aware forensic logging. All system logs and alerts are now automatically anchored with `lat`/`lng` coordinates using the last known telemetry position, enabling historical marker reconstruction on the map.
- **Documentation Alignment**: Synchronized all core specifications, field definitions, and thresholds to the v8.9.10 baseline.

## Recent Changes (v8.9.9)
- **Documentation Hardening & SoT alignment**: Synchronized `REQUIREMENTS_SOT.md` with `EngineConstants.kt`. Hardened GPS stall (60s), revival (120s), and muzzle window (2000ms) thresholds. Unified UI staleness and "Ghost Mode" gates to 10s.
- **Build Modernization**: Upgraded project toolchain to **Java 17** and aligned with **Android SDK 35** (Android 15).

## Recent Changes (v8.9.5)
- **Power Forensic Parity (Issue 192)**: Achieved absolute forensic parity for battery current (`currentMa`).
- **Viewer Background Location (Issue 189)**: Implemented 10s background polling for Viewer-side location.

## Project Core Features
- **Pure Logic Engine (:core:engine)**: Physically isolated tracking math, free from Android framework dependencies.
- **Zero-Lag Filtering**: Multi-layered sentinel (ImmFilter + Behavioral) for real-time jump rejection.
- **Physical Tamper Detection**: Acoustic fast-path, light-jump detection, and 3D orientation monitoring.
- **Anti-Stall Logic**: Monotonic clock regression guards and GPS stall detection.

## Technical Specifications
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin 2.0+ / Compose 1.7+
- **Architecture**: Clean Architecture with UseCases and Dagger Hilt
- **Map Engine**: osmdroid with persistent marker pooling
