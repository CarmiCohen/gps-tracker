# GPS Tracker: Technical Overview (v9.3.6)

GPS Tracker is a high-assurance, native Android application designed for asset protection and remote monitoring. Unlike consumer-grade tracking apps, it prioritizes **forensic continuity**, **high availability**, and **physical security** over simple location sharing.

## 1. Core Architecture (v9.3.6 Baseline)
The application follows a strict modular architecture to ensure logic integrity and prevent side-effect regressions.
-   **Hardened Engine (:core:engine)**: A pure JVM library module containing all tracking math, jump detection, and physical security logic. It is physically isolated from the Android framework and uses monotonic `TimeProvider` for all internal logic (Issue #441).
-   **Tracker Service**: The specialized "Black Box" role. Optimizes battery, sensor fidelity, and persistent logging. Features a 10Hz polling mode (Issue #432), **Escalated GPS Revival** (Issue #341), and **Xiaomi Recovery Pulse** (Issue #439).
-   **Viewer Service**: The "Monitoring" role. Handles real-time telemetry sync, HUD rendering, and remote command propagation. Now includes **Background Location Polling** for relative distance calculations.
-   **Decoupled UI**: The `MainViewModel` is decoupled into domain UseCases (Issue #322), ensuring high maintainability and testability. Includes **Ghost Mode** (R338) visual staleness indicators.

## 2. Security Tiers
### A. GPS Integrity (ImmFilter)
The system uses an **Interacting Multiple Model (IMM) Filter** within the `:core:engine` to process raw GPS data. This allows for zero-lag switching between `STATIONARY` and `KINEMATIC` states, filtering out radial noise (jitter) while maintaining responsiveness to real movement.

### B. Physical Sentinel
The Physical Sentinel is a zero-lag monitoring engine that detects unauthorized physical interactions:
-   **Muzzle Window**: A unified suppression window (Issue #191) gating sensor triggers during high-I/O sync operations. Includes 2000ms (Global), 500ms (A15 Hysteresis), and 5000ms (A15 Proximity debounce).
-   **Acoustic Fast-Path**: Detects rapid noise floor jumps (>40dB) indicative of forced entry.
-   **Light-Jump Detection**: Monitors the ambient light sensor for sudden exposure.
-   **3D Orientation**: Detects tilting (>15°) or lifting (>0.8m).
-   **Monotonic Hardware Locks**: Siren auto-stop (30s) and silence latches use monotonic `elapsedRealtime` to prevent clock-tamper bypass (Issue #441).

### C. Geofencing (GtoEngine)
The **GtoEngine** logic provides a high-confidence geofence gate. It uses a 6-sigma buffer (`GEOFENCE_BUFFER_MULT`) and **Bayesian Uncertainty Expansion** (15m/s moving, capped at 33.3m/s) to prevent false alarms from fix gaps while providing sub-second projection of fence breaches (Issue #460).

## 3. Forensic Telemetry (v9.3.6 Enhancements)
The system is built around "Forensic Continuity." Data is never simply "current"; it is always presented within its historical context via:
-   **Dual-Metric Spatial Anchor (Issue #325)**: All forensic logs and telemetry are anchored with both raw GPS `accuracy` and authoritative engine `maxAccuracy`. This ensures perfect parity between what the user sees on the map and the engine's internal confidence.
-   **Trajectory Forensic Parity (Issue #461)**: Points retroactively validated via "Trajectory Promotion" strictly preserve their original forensic metadata, ensuring a contiguous audit trail. (Formerly #435)
-   **Monotonic Timing (Issue #311)**: All forensic metrics and UI lockout thresholds use monotonic time to eliminate drift and manipulation.
-   **Power Forensic Parity (Issue #337)**: Full parity for battery current (`currentMa`) across all models, database (v50), and ribbons, ensuring remote power-deficit visibility.

## 4. Connectivity & Resilience
The app utilizes a custom socket-based protocol optimized for high-latency, unreliable mobile networks. It features:
-   **Triple-Lock Watchdog (Issue #456)**: Layered persistence using `WorkManager`, `AlarmManager`, and a 1s service heartbeat. (Formerly #366-R)
-   **Visual Watchdog**: A real-time countdown and **Ghost Mode** dimming (R338) showing exactly how long since the last verified packet was received.
-   **Automatic Recovery**: Specialized foreground services with sticky behavior and **Xiaomi Suppression Recovery** (15s detection / 60s recovery) (Issue #439).

## 5. Project Governance & Compliance Framework
To ensure that the app's high-assurance claims are auditable, the project adheres to a strict **Three-Tier Documentation Lifecycle**:
-   **Active Workspace (`issues.md`)**: Tracks only pending field validation tasks.
-   **Audit Archive (`STATUS/VERIFICATION_MANIFEST.md`)**: The definitive proof of implementation. Contains the Verification Manifest.
-   **Resolution Archive (`STATUS/RESOLUTION_ARCHIVE.md`)**: The complete record of historical resolutions.
-   **System Specification (`STATUS/SOT_MASTER_REQUIREMENTS.md`)**: The operational "Source of Truth" for constants and logic.
