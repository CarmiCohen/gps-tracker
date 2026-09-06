# SOT Master Requirements (Sep.06.17)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (49 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.23 Teardown Determinism (R923)**: **MANDATORY**. All hardware teardown sequences MUST join the forensic settling window using a managed `Job` to prevent async races and concurrent registration attempts during rapid service toggles (Sep.06.00).
*   **1.24 Hydration Safe-Mode (R924)**: **MANDATORY**. When the Hydration Watchdog triggers, the system MUST enter a "Safe Mode" that suppresses all signaling connection attempts via `CommunicationManager` (Sep.06.01).
*   **1.25 Clock Parity (R922)**: **MANDATORY**. All forensic indexing and backfill queries MUST use monotonic `SystemClock.elapsedRealtime()` as the primary key. Wall-clock time (UTC) MUST only be used for display and persistence metadata, never for interval calculation or sample correlation (Sep.06.17).

## 🧩 Functional Requirements (232 IDs)
*   **R-ID 260 (GNSS Revival Lifecycle Transparency)**: The system MUST emit definitive `Success` and `HardwareLock` events during GNSS recovery routines (Sep.05.30).
*   **R-ID 262 (Teardown Forensic Integrity)**: `HardwareProvider` MUST clear all pending energy footprint state upon session termination (Sep.06.00).
*   **R-ID 263 (Zero-Churn Forensic Buffering)**: The system MUST use a specialized `CircularStateBuffer` for all high-frequency forensic streams (SNR, Sensors, Snapshots) to eliminate GC pressure and locking overhead on budget hardware (Sep.06.17).
*   **R-ID 264 (Forensic Index Parity)**: `HistoryManager` backfill logic MUST query `HardwareProvider` using monotonic `rt` ranges to ensure continuity across system clock resets (Sep.06.17).
*   **R-ID 271 (Watchdog Safe-Mode Enforcement)**: The `CommunicationManager` MUST verify the `isSafeMode` state before initiating any relay connection (Sep.06.01).

*(Total: 49 Architectural Rules + 232 Functional R-IDs = 281 Items)*
