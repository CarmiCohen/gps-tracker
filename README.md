# GPS Tracker - High-Assurance Forensic Tracking (v9.3.12)

A modular, high-reliability Android tracking system designed for forensic continuity and behavioral analysis.

## 🚀 Core Features
- **Hilt-Based Dependency Injection**: Modern architectural foundation for services and components, ensuring testability and modularity (R978).
- **Temporal Authority**: Skew-immune GPS freshness logic using receipt-time deltas to ensure HUD/Map stability during clock drift (#075).
- **Screen-Off Optimization**: Dynamic GPS polling reduction (5s) when the device is locked to significantly extend battery life (R994).
- **Notification Throttling**: Dual-rate foreground service updates (1s active / 10s background) to balance visibility with efficiency (R993).
- **Pure Logic Engine**: Decoupled `:core:engine` module for high-assurance trajectory and violation analysis.
- **Stationary Anchor Hard-Lock**: Eliminates GPS drift (spaghetti trails) in Urban Canyons by clamping coordinates when stationary confidence is high (R990).
- **System-Wide Type Safety**: Native `Double` precision across the entire telemetry chain, eliminating conversion overhead (R014).
- **Forensic Unification**: Standardized forensic logging with **Log Spatial Anchors** and **Room Persistence** (R979).
- **Behavioral Sentinel**: Multi-sensor fusion for tamper detection, lift alerts, and chair-sit detection (R832).
- **Xiaomi & Samsung Hardening**: Implemented specialized background resilience for MIUI, HyperOS, and Samsung A15/S21 devices (R810-A15 / R971 / R996 / R997).
- **Monotonic Timing**: All logic gates and watchdog intervals use monotonic time for temporal integrity.

## 🏗 Architecture
The project follows a **Vault Architecture**, isolating the tracking math from the Android framework:
- **`:app`**: Android-specific UI, Persistence, and Service management.
- **`:core:engine`**: Pure Kotlin/Java tracking and physics logic.

## 📌 Branding
- **Primary Color**: JD Vivid Green (**#78BE20**) - Enforced across all layers (R799e).

## 🛠 Hardening Status
- **Baseline**: v9.3.12
- **Compliance**: Verified implementation of #075, R014, R018, R049, R325, R326, R400, R441, R832, R967, R968, R973, R976, R977, R978, R979, R980, R981, R985, R988, R989, R993, R994, R995, R996, R997, R799d, R799e.
- **Handover Baseline**: [Handover.md](Handover.md)

For detailed technical specifications, see the [STATUS/](STATUS/) directory.
