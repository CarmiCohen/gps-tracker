# GPS Tracker: Technical Overview (vAug.19.01)

GPS Tracker is a high-assurance, native Android application designed for asset protection and remote monitoring. Unlike consumer-grade tracking apps, it prioritizes **forensic continuity**, **high availability**, and **physical security** over simple location sharing.

## 1. Core Architecture (vAug.19.01 Baseline)
The application follows a strict modular architecture to ensure logic integrity and prevent side-effect regressions.
-   **Hardened Engine (:core:engine)**: A pure JVM library module containing all tracking math, jump detection, and physical security logic. It is physically isolated from the Android framework and uses monotonic `TimeProvider` for all internal logic (Issue #441).
-   **Device Service**: The specialized "Black Box" role. Optimizes battery, sensor fidelity, and persistent logging. Features a 10Hz polling mode, **Escalated GPS Revival** (Issue #124), and JdHardware stabilization (R212).
-   **Viewer Service**: The "Monitoring" role. Handles real-time telemetry sync, HUD rendering, and remote command propagation. Now includes **Background Location Polling** for relative distance calculations.
-   **Decoupled UI**: The `MainViewModel` is decoupled into domain UseCases (Issue #322), ensuring high maintainability and testability. Includes **Ghost Mode** (R338) visual staleness indicators.

## 2. Security Tiers
### A. GPS Integrity
The system uses an advanced filtering engine within the `:core:engine` to process raw GPS data. This allows for zero-lag switching between `STATIONARY` and `KINEMATIC` states, filtering out radial noise (jitter) while maintaining responsiveness to real movement.

### B. Physical Sentinel
The Physical Sentinel is a zero-lag monitoring engine that detects unauthorized physical interactions:
-   **Muzzle Window**: A unified suppression window (Issue #191) gating sensor triggers during high-I/O sync operations.
-   **Acoustic Fast-Path**: Detects rapid noise floor jumps (>40dB) indicative of forced entry.
-   **Light-Jump Detection**: Monitors the ambient light sensor for sudden exposure.
-   **3D Orientation**: Detects tilting (>15°) or lifting (>0.8m).
-   **Monotonic Hardware Locks**: Siren auto-stop (30s) and silence latches use monotonic `elapsedRealtime` to prevent clock-tamper bypass (Issue #441).

### C. Geofencing (GtoEngine)
The **GtoEngine** logic provides a high-confidence geofence gate. It uses a 6-sigma buffer (`GEOFENCE_BUFFER_MULT`) and **Bayesian Uncertainty Expansion** (15m/s moving) to prevent false alarms from fix gaps while providing sub-second projection of fence breaches (Issue #460).

## 3. Forensic Telemetry (vAug.19.01 Enhancements)
The system is built around "Forensic Continuity." Data is never simply "current"; it is always presented within its historical context via:
-   **Dual-Metric Spatial Anchor (Issue #325)**: All forensic logs and telemetry are anchored with both raw GPS `accuracy` and authoritative engine `maxAccuracy`.
-   **Trajectory Forensic Parity**: Points retroactively validated via "Trajectory Promotion" strictly preserve their original forensic metadata, ensuring a contiguous audit trail.
-   **Monotonic Timing**: All forensic metrics and UI lockout thresholds use monotonic time to eliminate drift and manipulation.
-   **Forensic Spill-Buffer (R743)**: High-frequency telemetry is captured in a memory-mapped spill-buffer using a 96-byte compressed format to ensure integrity during power loss.

## 4. Connectivity & Resilience
The app utilizes a custom socket-based protocol optimized for high-latency, unreliable mobile networks. It features:
-   **Triple-Lock Watchdog**: Layered persistence using `WorkManager`, `AlarmManager`, and a 1s service heartbeat.
-   **Visual Watchdog**: A real-time countdown and **Ghost Mode** dimming (R338) showing exactly how long since the last verified packet was received.
-   **Automatic Recovery**: Specialized foreground services with sticky behavior and **Xiaomi Suppression Recovery**.

## 5. Project Governance & Compliance Framework
To ensure that the app's high-assurance claims are auditable, the project adheres to a strict **Documentation Lifecycle**:
-   **Active Workspace (`issues.md`)**: Tracks current hardening and pending tasks.
-   **Source of Truth (`STATUS/SOT_MASTER_REQUIREMENTS.md`)**: The operational authority for constants and logic.
-   **Resolution Archive (`STATUS/RESOLUTION_ARCHIVE.md`)**: The complete record of historical resolutions.
-   **Verification Manifest (`STATUS/VERIFICATION_MANIFEST.md`)**: Formal proof of implementation for audits.
