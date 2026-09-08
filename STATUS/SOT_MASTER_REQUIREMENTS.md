# SOT Master Requirements (Sep.08.12)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (53 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.23 Teardown Determinism (R923)**: **MANDATORY**. All hardware teardown sequences MUST join the forensic settling window using a managed `Job` to prevent async races and concurrent registration attempts during rapid service toggles (Sep.06.30).
*   **1.24 Hydration Watchdog Trigger (R924)**: **MANDATORY**. When the Hydration Watchdog triggers, the system MUST enter a "Safe Mode" that suppresses all signaling connection attempts via `CommunicationManager` (Sep.06.01).
*   **1.25 Clock Parity (R922)**: **MANDATORY**. All forensic indexing and backfill queries MUST use monotonic `SystemClock.elapsedRealtime()` as the primary key. Wall-clock time (UTC) MUST only be used for display and persistence metadata, never for interval calculation or sample correlation (Sep.06.17).
*   **1.26 Forensic Separation (R922b)**: **MANDATORY**. Specialized hardware audits (GNSS jitter, sensor rates, energy footprints) MUST be decoupled from hardware bridge implementations (e.g., `HardwareProvider`) into dedicated forensic auditors to maintain bridge leaness and SRP (Sep.06.17).
*   **1.27 Viewer Background Persistence (R926)**: **MANDATORY**. The `ViewerService` MUST utilize `specialUse` FGS type on Android 14+ and maintain a 30s hardware "Poke" rhythm to prevent Samsung-specific background suspension (Sep.06.45).
*   **1.28 Service Mutual Exclusivity (R975)**: **MANDATORY**. The application MUST ensure that only one role-specific foreground service (Tracker or Viewer) is active at any time. Mode transitions MUST explicitly terminate the previous service and clear all in-memory telemetry state (activity timestamps/buffers) before initiating the next to prevent cross-role status ghosting (Sep.07.82).
*   **1.29 Reference-Counted Hardware (R975b)**: **MANDATORY**. `HardwareProvider` MUST utilize internal reference counting to manage the lifecycle of physical sensors and GNSS status callbacks. Teardown sequences MUST be suppressed if an active user (Tracker or Viewer) remains, ensuring continuity during rapid mode transitions (Sep.08.00).

## 🧩 Functional Requirements (243 IDs)
*   **R-ID 279 (Monotonic Signaling Hardening)**: The system MUST include the monotonic `rt` (elapsedRealtime) field in all `RealtimeStatus` Protobuf payloads to eliminate heuristic drift in remote HUD signaling and resolve false-positive "Red-Lock" states (Sep.08.10).
*   **R-ID 280 (Forensic Auditor Consolidation)**: Shared stability audit logic (Reliability % / GNSS Jitter) MUST be centralized in `ForensicAuditor` to ensure consistent reporting across Tracker and Viewer roles (Sep.08.12).
*   **R-ID 281 (Hydration Watchdog Recovery)**: The system MUST implement active recovery for hydration stalls. If stuck at Level 2, the system MUST force a reset and restart of the `LifecycleHydrationManager` sequence (Sep.08.12).

*(Total: 53 Architectural Rules + 243 Functional R-IDs = 296 Items)*
