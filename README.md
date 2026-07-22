# GPS Tracker - High-Assurance Forensic Tracking (July.22.05)

A modular, high-reliability Android tracking system designed for forensic continuity and behavioral analysis.

## 🚀 Core Features
- **Hilt Universal Authority (R120b)**: Modern, type-safe dependency injection across all services, repositories, and viewmodels. Legacy manual DI is decommissioned.
- **API Synchronization Authority (R999b)**: Strict telemetry signature parity between the background service layer and core logic engine.
- **Temporal Authority**: Skew-immune GPS freshness logic using receipt-time deltas (#075).
- **Forensic Ribbon Continuity**: Reconstruction of monotonic timelines across process boundaries to ensure "Black Gap" visualization fidelity.
- **Barometric Lift Hardening**: Precision motion analysis utilizing synchronized barometer EMA deltas.
- **Hardware-Specific Hardening**: Specialized background resilience for Samsung A15/S21 and Xiaomi/HyperOS devices.

## 🏗 Architecture
The project follows a **Vault Architecture**, isolating tracking math from the Android framework:
- **`:app`**: Hilt-powered UI, Room Persistence, and Foreground Service management.
- **`:core:engine`**: Pure Kotlin/Java tracking and physics logic.

## 🛠 Hardening Status
- **Current Release**: July.22.05 (Hilt/Forensic Baseline)
- **Simplification Status**: [SIMPLIFICATION_PLAN.md](SIMPLIFICATION_PLAN.md)

For detailed technical specifications and verification proof, see the [STATUS/](STATUS/) directory.
