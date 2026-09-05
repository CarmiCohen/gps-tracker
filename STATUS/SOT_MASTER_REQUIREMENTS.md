# SOT Master Requirements (Sep.05.11)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (42 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.17 Robust Battery Navigation (R896)**: **MANDATORY**. Battery optimization exemption intents MUST use `Uri.fromParts("package", pkg, null)` for URI encoding and implement a multi-tier fallback (Sep.02.40).
*   **1.18 Log Spillage Protection (R759)**: **MANDATORY**. All application-level logging MUST use `Timber`. Direct calls to `android.util.Log` or `System.out.println` are strictly prohibited (Sep.02.50).

## 🧩 Functional Requirements (221 IDs)
*   **R-ID 238 (Model Unification)**: The application MUST use `LocationUpdate` as the single source of truth for location data across both the Core Engine and UI layers (Sep.03.01).
*   **R-ID 239 (Signaling Consolidation)**: The communication layer MUST expose a unified `transmit(TrackerStatus)` entry point that internally handles role-based serialization (Protobuf/JSON) (Sep.02.70).
*   **R-ID 240 (Tracker HUD Telemetry)**: `TrackerService` MUST publish telemetry to the repository every tick, regardless of GPS fix status, to ensure the local HUD remains live (Sep.03.15).
*   **R-ID 241 (Atomic Activation)**: The system MUST atomically persist `IS_SYSTEM_ACTIVE_KEY = true` during role selection to ensure that background workers are unblocked (Sep.02.66).
*   **R-ID 242 (Recovery Pipeline)**: The system MUST implement a reactive signal-response loop between the Activity lifecycle and the UI layer (Sep.02.66).
*   **R-ID 243 (Status Visibility)**: `GlobalStatusBar` MUST propagate the `isSystemActive` flag to all child indicators (Sep.02.68).
*   **R-ID 244 (ContextShadow Automation)**: The system MUST automatically inject the `ContextShadow` wrapper into all high-frequency singleton services via Hilt (Sep.02.70).
*   **R-ID 245 (Protobuf Unification)**: The system MUST use a centralized mapping utility for all Protobuf-to-domain transformations (Sep.02.70).
*   **R-ID 246 (SYS Badge Lifecycle)**: The system MUST atomically deactivate the "SYS" tracking indicator upon session termination (Sep.02.70).
*   **R-ID 247 (Map Event Unification)**: `MainViewModel` MUST delegate all map-related UI events to `MapUseCase` and `HomePointUseCase` (Sep.02.76).
*   **R-ID 248 (Forensic Signal Latch)**: The system MUST implement a 5-second grace period (`BUDGET_HARDWARE_SIGNAL_GRACE_MS`) for Signal Loss auditing on budget hardware (e.g., A15) and correlate triggers with relay recovery states to prevent false alerts during telemetry gaps (Sep.03.100).
*   **R-ID 249 (A15 Background Persistence)**: The system MUST maintain radio active state on Samsung A15 devices by pulsing the hardware every 30s and preventing GPS polling relaxation below 10s (`SUSPICIOUS_GPS_POLLING_MS`) when moving with the screen off to ensure UI freshness (Sep.03.111).
*   **R-ID 250 (Field Test Readiness)**: The system MUST support real-world field testing by defaulting to high-accuracy GNSS and ensuring peer identities are pre-aligned ("T" and "V") for zero-config connectivity (Sep.03.120).
*   **R-ID 251 (Signaling Transport Robustness)**: The signaling layer MUST allow default transport negotiation (polling-to-websocket) to ensure connection stability across diverse network environments and hardware architectures (Sep.04.30).
*   **R-ID 252 (GNSS Zombie Recovery)**: The system MUST trigger a hardware-level revival pulse (location update restart) when GNSS visibility drops to zero (`SIGNAL_LOSS`) or stalls for more than 120s on Samsung hardware to remediate native stack "Zombie States" (Sep.04.30).
*   **R-ID 253 (Protobuf Identity Parity)**: The signaling layer MUST apply alias-aware ID mapping (via `SignalingConstants.getTransmissionId`) to all binary Protobuf payloads to ensure protocol parity with the JSON path and prevent peer-to-peer handshake failures (Sep.04.30).
*   **R-ID 254 (Rolling Deployment Sync)**: The signaling layer MUST re-broadcast its identity (Join payload) every 60s while connected to ensure peer discovery and synchronization during rolling deployments, remediating "Red SRV" lag on budget hardware (Sep.04.40).
*   **R-ID 255 (Hydration Navigation Guard)**: The UI layer MUST NOT navigate to the Landing route if `isSystemActive` is true, regardless of `appMode` state, to prevent accidental service termination via BackHandler during hydration gaps (Sep.05.10).
*   **R-ID 256 (High Frequency Sensor Authority)**: The application MUST declare `HIGH_SAMPLING_RATE_SENSORS` permission in the manifest to support high-fidelity IMU data collection for forensic analysis on Target SDK 35+ (Sep.05.11).

*(Total: 42 Architectural Rules + 221 Functional R-IDs = 263 Items)*
