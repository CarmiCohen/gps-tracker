# GPS-Tracker (Pure Native APK)

**Version: 8.9.27**

This project is a **Pure Native Android Application** built with Kotlin and Jetpack Compose, designed for high-precision background tracking and remote monitoring in demanding environments.

## ⚖️ Project Governance & Documentation Standard
To maintain **Forensic Integrity**, we follow a strict documentation lifecycle:
- **Active Tracker**: [issues.md](issues.md) — Only open issues and pending hardening tasks.
- **Audit Archive**: [COMPLIANCE.md](COMPLIANCE.md) — Verified resolutions and compliance proof.
- **Engineering Standard**: [CONTRIBUTING.md](CONTRIBUTING.md) — Rules for moving tasks between documents.
- **Source of Truth**: [DOCS/REQUIREMENTS_SOT.md](DOCS/REQUIREMENTS_SOT.md) — Operational specifications.

## Recent Changes (v8.9.27)
- **Documentation Reorganization**: Unified all legacy history files into `COMPLIANCE.md`.
- **Engineering Rule Enforcement**: Established the 3-tier documentation system to support forensic continuity.
- **Log Spatial Anchor (Issue #208)**: Implemented coordinate-aware forensic logging.

## Project Core Features
- **Pure Logic Engine (:core:engine)**: Physically isolated tracking math, free from Android framework dependencies.
- **Zero-Lag Filtering**: Multi-layered sentinel (ImmFilter + Behavioral) for real-time jump rejection.
- **Physical Tamper Detection**: Acoustic fast-path, light-jump detection, and 3D orientation monitoring.
- **High-Availability**: Automated GPS revival and Xiaomi/Samsung background stabilization.

## Technical Specifications
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin 2.0+ / Compose 1.7+
- **Architecture**: Clean Architecture with Feature-based UseCases
- **Map Engine**: osmdroid with persistent marker pooling
