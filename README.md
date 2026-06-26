# GPS-Tracker (Pure Native APK)

**Version: 8.9.37**

This project is a **Pure Native Android Application** built with Kotlin and Jetpack Compose, designed for high-precision background tracking and remote monitoring in demanding environments.

## ⚖️ Project Governance & Documentation Standard
To maintain **Forensic Integrity**, we follow a strict documentation lifecycle:
- **Active Tracker**: [STATUS/issues.md](STATUS/issues.md) — Only open issues and pending hardening tasks.
- **Audit Archive**: [STATUS/compliance.md](STATUS/compliance.md) — Verified resolutions and compliance proof.
- **Engineering Standard**: [DOCS/CONTRIBUTING.md](DOCS/CONTRIBUTING.md) — Rules for moving tasks between documents.
- **Source of Truth**: [STATUS/requirements_sot.md](STATUS/requirements_sot.md) — Operational specifications.
- **Organization**: [DOCS/DOCUMENTATION_ORGANIZATION.md](DOCS/DOCUMENTATION_ORGANIZATION.md) — Detailed hierarchy.

## Recent Changes (v8.9.37)
- **Documentation Hardening (Issue #312)**: Finalized Source of Truth alignment across all markdown files.
- **Forensic Unification**: Standardized forensic logging with **Log Spatial Anchors** and **Ghost Mode** staleness indicators (Issue #193).
- **GtoEngine Implementation**: Integrated sliding-window trajectory optimization for heavy asset tracking (Issue #309).
- **Xiaomi & Samsung Hardening**: Implemented specialized background resilience for MIUI and Samsung hardware (Issue #148/190).

## Project Core Features
- **Pure Logic Engine (:core:engine)**: Physically isolated tracking math, free from Android framework dependencies.
- **Zero-Lag Filtering**: Multi-layered sentinel (ImmFilter + Behavioral) for real-time jump rejection.
- **Physical Tamper Detection**: Acoustic fast-path, light-jump detection, and 3D orientation monitoring.
- **High-Availability**: Automated GPS revival and specialized background stabilization.

## Technical Specifications
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin 2.0+ / Compose 1.7+
- **Architecture**: Clean Architecture with Feature-based UseCases
- **Map Engine**: osmdroid with persistent marker pooling
