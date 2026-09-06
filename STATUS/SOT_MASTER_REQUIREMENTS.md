# SOT Master Requirements (Sep.06.33)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (50 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.23 Teardown Determinism (R923)**: **MANDATORY**. All hardware teardown sequences MUST join the forensic settling window using a managed `Job` to prevent async races and concurrent registration attempts during rapid service toggles (Sep.06.30).
*   **1.24 Hydration Watchdog Trigger (R924)**: **MANDATORY**. When the Hydration Watchdog triggers, the system MUST enter a "Safe Mode" that suppresses all signaling connection attempts via `CommunicationManager` (Sep.06.01).
*   **1.25 Clock Parity (R922)**: **MANDATORY**. All forensic indexing and backfill queries MUST use monotonic `SystemClock.elapsedRealtime()` as the primary key. Wall-clock time (UTC) MUST only be used for display and persistence metadata, never for interval calculation or sample correlation (Sep.06.17).
*   **1.26 Forensic Separation (R922b)**: **MANDATORY**. Specialized hardware audits (GNSS jitter, sensor rates, energy footprints) MUST be decoupled from hardware bridge implementations (e.g., `HardwareProvider`) into dedicated forensic auditors to maintain bridge leaness and SRP (Sep.06.17).

## 🧩 Functional Requirements (236 IDs)
*   **R-ID 256 (Sensor Rate Auditing)**: The system MUST perform a runtime audit of accelerometer sampling rates to ensure efficacy on high-Target-SDK devices (Sep.05.29).
*   **R-ID 259 (Energy Footprint Verdicts)**: The system MUST quantify the power cost of GNSS revival pulses via mA delta and temperature rise calculation (Sep.05.30).
*   **R-ID 260 (GNSS Revival Lifecycle Transparency)**: The system MUST emit definitive `Success` and `HardwareLock` events during GNSS recovery routines (Sep.05.30).
*   **R-ID 262 (Teardown Forensic Integrity)**: `HardwareProvider` MUST clear all pending energy footprint state upon session termination (Sep.06.00).
*   **R-ID 263 (Zero-Churn Forensic Buffering)**: The system MUST use a specialized `CircularStateBuffer` for all high-frequency forensic streams (SNR, Sensors, Snapshots) to eliminate GC pressure and locking overhead on budget hardware (Sep.06.17).
*   **R-ID 264 (Forensic Index Parity)**: `HistoryManager` backfill logic MUST query `HardwareProvider` using monotonic `rt` ranges to ensure continuity across system clock resets (Sep.06.17).
*   **R-ID 267 (A15 GNSS Throttling)**: The hardware layer MUST dynamically throttle GNSS satellite updates to 5000ms on A15 devices when high system load or Mali driver anomalies are detected to prevent UI jank and thermal escalation (Sep.06.20).
*   **R-ID 271 (Watchdog Safe-Mode Enforcement)**: The `CommunicationManager` MUST verify the `isSafeMode` state before initiating any relay connection (Sep.06.01).
*   **R-ID 272 (Hardware Lock Parity)**: The system MUST propagate the `gpsHardwareLock` signal across all roles (Tracker/Viewer) via Protobuf and local persistence to ensure HUD awareness of hardware failure (Sep.06.31).
*   **R-ID 273 (Synchronous Hardware Initialization)**: `HardwareProvider.start()` MUST suspend until any active `teardownJob` completes to prevent race conditions during rapid session restarts (Sep.06.30).
*   **R-ID 274 (Mali Exit Hysteresis)**: The hardware layer MUST enforce a 10s cooldown/hysteresis period after an anomaly state (High Load or Mali Anomaly) clears before returning to standard GNSS sampling rates to prevent sampling jitter on budget hardware (Sep.06.33).

*(Total: 50 Architectural Rules + 236 Functional R-IDs = 286 Items)*
