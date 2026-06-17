# GPS Tracker: Technical Overview (v8.8.35)

GPS Tracker is a high-assurance, native Android application designed for asset protection and remote monitoring. Unlike consumer-grade tracking apps, it prioritizes **forensic continuity** and **physical security** over simple location sharing.

## 1. Core Architecture (v8.8.35 Baseline)
The application follows a strict modular architecture to ensure logic integrity and prevent side-effect regressions.
-   **Sacred Engine (:core:engine)**: A pure JVM library module containing all tracking math, jump detection, and physical security logic. It is physically isolated from the Android framework and uses injected `TimeProvider` for all logic.
-   **Tracker Service**: The specialized "Black Box" role. Optimizes battery, sensor fidelity, and persistent logging. Features a 10Hz polling mode and **Escalated GPS Revival** (Issue 124) for high-availability tracking.
-   **Viewer Service**: The "Monitoring" role. Handles real-time telemetry sync, HUD rendering, and remote command propagation.
-   **Decoupled UI**: The `MainViewModel` is decoupled into domain UseCases, ensuring high maintainability and testability.

## 2. Security Tiers
### A. GPS Integrity (ImmFilter)
The system uses an **Interacting Multiple Model (IMM) Filter** within the `:core:engine` to process raw GPS data. This allows for zero-lag switching between `STATIONARY` and `KINEMATIC` states, filtering out radial noise (jitter) while maintaining responsiveness to real movement.

### B. Physical Sentinel
The Physical Sentinel is a zero-lag monitoring engine that detects unauthorized physical interactions:
-   **Muzzle Window**: A 500ms suppression window gates sensor triggers during high-I/O sync operations.
-   **Acoustic Fast-Path**: Detects rapid noise floor jumps (>40dB) indicative of forced entry.
-   **Light-Jump Detection**: Monitors the ambient light sensor for sudden exposure.
-   **3D Orientation**: Detects tilting (>15°) or lifting (>0.8m).
-   **Tamper Logic**: Integrated proximity and hardware vibration analysis to identify physical displacement.

### C. Geofencing (GtoEngine)
The **GtoEngine** logic provides a high-confidence geofence gate. It uses a 6-sigma buffer (`GEOFENCE_BUFFER_MULT`) to prevent false alarms from low-accuracy coordinates while providing sub-second projection of fence breaches.

## 3. Forensic Telemetry (v8.8.35 Enhancements)
The system is built around "Forensic Continuity." Data is never simply "current"; it is always presented within its historical context via:
-   **Monotonic Timing**: All forensic metrics and UI lockout thresholds (Issue 125) use `TimeProvider.elapsedRealtime()` to ensure absolute accuracy and eliminate drift caused by system clock adjustments.
-   **Escalated Revival**: If the GPS hardware stalls, the system attempts a retry loop every 5 minutes and escalates to a **CRITICAL forensic alert** after 3 failed attempts.
-   **Analytical Ribbons**: High-density sparklines (4M to 7D) showing SNR, ambient noise, light, vibration, and connectivity patterns.
-   **Identity Integrity**: Every telemetry entry contains a mandatory `role` field ("tracker" or "viewer") for forensic audit stability. The legacy `ver` field has been removed in favor of a simplified forensic model.

## 4. Connectivity & Resilience
The app utilizes a custom socket-based protocol optimized for high-latency, unreliable mobile networks. It features:
-   **Visual Watchdog**: A real-time countdown showing exactly how long since the last verified packet was received.
-   **Automatic Recovery**: Specialized foreground services with sticky behavior and system-level watchdog monitoring.
-   **I/O Efficiency**: Counter-based database pruning and batch-flushing ensure minimal background battery impact.
