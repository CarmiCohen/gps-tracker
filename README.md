# GPS Tracker - High-Assurance Forensic Tracking (July.16.22)

A modular, high-reliability Android tracking system designed for forensic continuity and behavioral analysis.

## 🚀 Core Features
- **Manual Dependency Injection (R406c)**: Clean, compile-time safe architecture using a central `AppContainer` registry for all singletons and repositories.
- **API Synchronization Authority (R999b)**: Strict telemetry signature parity between the background service layer and core logic engine, ensuring type-safe data flow.
- **Temporal Authority**: Skew-immune GPS freshness logic using receipt-time deltas (#075).
- **Map Follow Mode Persistence**: User focus intent (Tracker/Viewer/Auto) is respected during auto-centering events (#078).
- **Barometric Lift Hardening**: Lift detection utilizes synchronized barometer EMA deltas for high-precision motion analysis (R999b).
- **Xiaomi & Samsung Hardening**: Implemented specialized background resilience for MIUI, HyperOS, and Samsung A15/S21 devices.

## 🏗 Architecture
The project follows a **Vault Architecture**, isolating the tracking math from the Android framework:
- **`:app`**: Android-specific UI, Persistence, and Service management.
- **`:core:engine`**: Pure Kotlin/Java tracking and physics logic.

## 🛠 Hardening Status
- **Current Release**: July.16.22
- **Simplification Status**: [SIMPLIFICATION_PLAN.md](SIMPLIFICATION_PLAN.md)

For detailed technical specifications, see the [STATUS/](STATUS/) directory.
