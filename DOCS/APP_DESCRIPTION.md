# GPS Tracker: Technical Overview (v8.9.37)

GPS Tracker is a high-assurance, native Android application designed for asset protection and remote monitoring. Unlike consumer-grade tracking apps, it prioritizes **forensic continuity**, **high availability**, and **physical security** over simple location sharing.

## 1. Core Architecture (v8.9.37 Baseline)
The application follows a strict modular architecture to ensure logic integrity and prevent side-effect regressions.
-   **Sacred Engine (:core:engine)**: A pure JVM library module containing all tracking math, jump detection, and physical security logic. It is physically isolated from the Android framework and uses injected `TimeProvider` for all logic.
-   **Tracker Service**: The specialized "Black Box" role. Optimizes battery, sensor fidelity, and persistent logging. Features a 10Hz polling mode, **Escalated GPS Revival** (Issue #124), and **Muzzle Window Hardening** (Issue #191).
-   **Viewer Service**: The "Monitoring" role. Handles real-time telemetry sync, HUD rendering, and remote command propagation. Now includes **Background Location Polling** (Issue #189) for relative distance calculations.
-   **Decoupled UI**: The `MainViewModel` is decoupled into domain UseCases (Issue #322 / Formerly #115), ensuring high maintainability and testability. Includes **Ghost Mode** (Issue #193) visual staleness indicators.

## 2. Security Tiers
### A. GPS Integrity (ImmFilter)
The system uses an **Interacting Multiple Model (IMM) Filter** within the `:core:engine` to process raw GPS data. This allows for zero-lag switching between `STATIONARY` and `KINEMATIC` states, filtering out radial noise (jitter) while maintaining responsiveness to real movement.

### B. Physical Sentinel
The Physical Sentinel is a zero-lag monitoring engine that detects unauthorized physical interactions:
-   **Muzzle Window**: A unified suppression window (Issue #191) gating sensor triggers during high-I/O sync operations. Includes 2000ms (Global), 500ms (A15 Hysteresis), and 5000ms (A15 Proximity debounce).
-   **Acoustic Fast-Path**: Detects rapid noise floor jumps (>40dB) indicative of forced entry.
-   **Light-Jump Detection**: Monitors the ambient light sensor for sudden exposure.
-   **3D Orientation**: Detects tilting (>15°) or lifting (>0.8m).
-   **Tamper Logic**: Integrated proximity and hardware vibration analysis to identify physical displacement.

### C. Geofencing (GtoEngine)
The **GtoEngine** logic provides a high-confidence geofence gate. It uses a 6-sigma buffer (`GEOFENCE_BUFFER_MULT`) to prevent false alarms from low-accuracy coordinates while providing sub-second projection of fence breaches.

## 3. Forensic Telemetry (v8.9.37 Enhancements)
The system is built around "Forensic Continuity." Data is never simply "current"; it is always presented within its historical context via:
-   **Log Spatial Anchor (Issue #208)**: All forensic logs and critical alerts are automatically anchored with `lat`/`lng` coordinates using the last known telemetry position. This enables historical marker reconstruction on the Map even for events that occurred during relay blackouts.
-   **Monotonic Timing (Issue #311)**: All forensic metrics and UI lockout thresholds use `TimeProvider.elapsedRealtime()` to ensure absolute accuracy and eliminate drift. (Formerly #283)
-   **High-Availability Revival**: If the GPS hardware stalls, the system attempts a retry loop every 120s (`GPS_REVIVAL_RETRY_INTERVAL_MS`) and escalates to a **CRITICAL forensic alert** after 3 failures.
-   **Power Forensic Parity (Issue #192)**: Full parity for battery current (`currentMa`) across all models, database (v35), and ribbons, ensuring remote power-deficit visibility.
-   **SIT Acknowledgement (Issue #194)**: Discrete "sitting" events are synchronized via an acknowledged loop to prevent loss during blackouts.
-   **Analytical Ribbons**: High-density sparklines (4M to 7D) showing SNR, ambient noise, light, vibration, power, and connectivity patterns.

## 4. Connectivity & Resilience
The app utilizes a custom socket-based protocol optimized for high-latency, unreliable mobile networks. It features:
-   **Visual Watchdog**: A real-time countdown and **Ghost Mode** dimming showing exactly how long since the last verified packet was received.
-   **Automatic Recovery**: Specialized foreground services with sticky behavior, system-level watchdog monitoring, and **Xiaomi Boot Resilience** (Issue #190).
-   **I/O Efficiency**: Counter-based database pruning and batch-flushing ensure minimal background battery impact.

## 5. Project Governance & Compliance Framework
To ensure that the app's high-assurance claims are auditable, the project adheres to a strict **Three-Tier Documentation Lifecycle**:
-   **Active Workspace (`STATUS/issues.md`)**: Tracks only open and pending hardening tasks.
-   **Audit Archive (`STATUS/compliance.md`)**: The definitive proof of implementation. Contains the Verification Manifest and the complete Resolution Archive.
-   **System Specification (`STATUS/requirements_sot.md`)**: The operational "Source of Truth" for constants and logic.
This framework ensures that forensic history is never lost and that the active codebase is always measurable against its compliance requirements. For further details on this lifecycle, see `DOCS/CONTRIBUTING.md`.
