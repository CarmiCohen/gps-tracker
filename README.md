# GPS Tracker - High-Assurance Forensic Tracking (v8.9.60)

A modular, high-reliability Android tracking system designed for forensic continuity and behavioral analysis.

## 🚀 Core Features
- **Pure Logic Engine**: Decoupled `:core:engine` module for high-assurance trajectory and violation analysis.
- **Forensic Unification**: Standardized forensic logging with **Log Spatial Anchors** and **Ghost Mode UX** (Issue #338).
- **Behavioral Sentinel**: Multi-sensor fusion for tamper detection, lift alerts, and chair-sit detection (Issue #459).
- **Xiaomi & Samsung Hardening**: Implemented specialized background resilience for MIUI, HyperOS, and Samsung A15/S21 devices (Issue #190 / Issue #363).
- **Monotonic Timing**: All logic gates and watchdog intervals use monotonic time for temporal integrity (Issue #311).
- **Authoritative Spatial Anchoring**: Dual-metric uncertainty tracking (Accuracy vs. Max-Accuracy) (Issue #325).

## 🏗 Architecture
The project follows a **Vault Architecture**, isolating the tracking math from the Android framework:
- **`:app`**: Android-specific UI, Persistence, and Service management.
- **`:core:engine`**: Pure Kotlin/Java tracking and physics logic.

## 📌 Branding
- **Primary Color**: JD Branding Green (**#367C2B**) - Enforced across all layers (R865/R866).

## 🛠 Hardening Status
- **Baseline**: v8.9.60
- **Compliance**: Verified implementation of R568a, R729, R800, R832, R880, R944, R945.
- **Handover Baseline**: [Handover.md](Handover.md)

For detailed technical specifications, see the [DOCS](DOCS/) directory.
