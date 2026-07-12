# GPS Tracker - High-Assurance Forensic Tracking (v9.3.16)

A modular, high-reliability Android tracking system designed for forensic continuity and behavioral analysis.

## 🚀 Core Features
- **API Synchronization Authority (R999b)**: Strict telemetry signature parity between the background service layer and core logic engine, ensuring type-safe data flow.
- **Hilt-Based Dependency Injection**: Modern architectural foundation for services and components (R978).
- **Temporal Authority**: Skew-immune GPS freshness logic using receipt-time deltas (#075).
- **Map Follow Mode Persistence**: User focus intent (Tracker/Viewer/Auto) is respected during auto-centering events (#078).
- **Barometric Lift Hardening**: Lift detection utilizes synchronized barometer EMA deltas for high-precision motion analysis (R999b).
- **Xiaomi & Samsung Hardening**: Implemented specialized background resilience for MIUI, HyperOS, and Samsung A15/S21 devices.

## 🏗 Architecture
The project follows a **Vault Architecture**, isolating the tracking math from the Android framework:
- **`:app`**: Android-specific UI, Persistence, and Service management.
- **`:core:engine`**: Pure Kotlin/Java tracking and physics logic.

## 🛠 Hardening Status
- **Current Release**: v9.3.16
- **Handover Baseline**: [Handover.md](Handover.md)

For detailed technical specifications, see the [STATUS/](STATUS/) directory.
