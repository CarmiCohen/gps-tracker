# GPS Tracker - High-Assurance Forensic Tracking (July.23.05)

A modular, high-reliability Android tracking system designed for forensic continuity, kinematic precision, and behavioral analysis.

## 🚀 Core Features
- **Hardening Finality (July.23.04)**: Refined stationary anchor convergence (#533) and enforced global type safety with `Double` precision (R999).
- **Hilt Universal Authority (R120b)**: Modern, type-safe dependency injection across all services and core components.
- **Acoustic Duty Cycle (R810-L2)**: Power-optimized forensic monitoring with a 20% duty cycle (2s ON / 8s OFF) and flicker-free FGS consistency (#531).
- **Temporal & Forensic Integrity**: Dual-time strategy using monotonic `rt` for logic and wall-clock `ts` for logging. Skew-immune GPS freshness logic (#075).
- **Hardware-Specific Hardening**: Upgraded "Stay-Alive" pulse for budget hardware (Samsung A15) to prevent OS-level background eviction.

## 🏗 Architecture
The project follows a **Vault Architecture**, isolating tracking physics from the Android framework:
- **`:app`**: Hilt-powered UI, DataStore/Room persistence, and Background Service management.
- **`:core:engine`**: Pure Kotlin/Java tracking and physics logic.

## 📊 Status & Governance
The authoritative state of the project is managed in the [STATUS/](STATUS/) directory:
- **Project Dashboard**: [issues.md](issues.md) (Active tasks and recent resolutions)
- **Historical Archive**: [STATUS/RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md) (Unified record of 356 resolutions)
- **Source of Truth**: [STATUS/SOT_MASTER_REQUIREMENTS.md](STATUS/SOT_MASTER_REQUIREMENTS.md) (Definitive specifications)
- **Governance Policy**: [STATUS/README.md](STATUS/README.md) (Documentation integrity rules)

For technical handover details, see [Handover.md](Handover.md).
