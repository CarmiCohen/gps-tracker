# SOT Master Requirements (Sep.07.70)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (51 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.23 Teardown Determinism (R923)**: **MANDATORY**. All hardware teardown sequences MUST join the forensic settling window using a managed `Job` to prevent async races and concurrent registration attempts during rapid service toggles (Sep.06.30).
*   **1.24 Hydration Watchdog Trigger (R924)**: **MANDATORY**. When the Hydration Watchdog triggers, the system MUST enter a "Safe Mode" that suppresses all signaling connection attempts via `CommunicationManager` (Sep.06.01).
*   **1.25 Clock Parity (R922)**: **MANDATORY**. All forensic indexing and backfill queries MUST use monotonic `SystemClock.elapsedRealtime()` as the primary key. Wall-clock time (UTC) MUST only be used for display and persistence metadata, never for interval calculation or sample correlation (Sep.06.17).
*   **1.26 Forensic Separation (R922b)**: **MANDATORY**. Specialized hardware audits (GNSS jitter, sensor rates, energy footprints) MUST be decoupled from hardware bridge implementations (e.g., `HardwareProvider`) into dedicated forensic auditors to maintain bridge leaness and SRP (Sep.06.17).
*   **1.27 Viewer Background Persistence (R926)**: **MANDATORY**. The `ViewerService` MUST utilize `specialUse` FGS type on Android 14+ and maintain a 30s hardware "Poke" rhythm to prevent Samsung-specific background suspension (Sep.06.45).
*   **1.28 Service Mutual Exclusivity (R975)**: **MANDATORY**. The application MUST ensure that only one role-specific foreground service (Tracker or Viewer) is active at any time. Mode transitions MUST explicitly terminate the previous service before initiating the next to prevent telemetry leakage (Sep.07.70).

## 🧩 Functional Requirements (239 IDs)
*   **R-ID 276 (A15 Viewer Parity)**: The Viewer role MUST maintain parity with Tracker GPS reception by implementing 30s monotonic WakeLock pokes, adaptive 2000ms polling when the UI is foregrounded, and a full Stability Audit loop (Reliability % / GNSS Jitter / Energy Footprints) (Sep.06.55).
*   **R-ID 277 (Service Transition Integrity)**: Mode transitions triggered via `MainActivity` MUST perform a synchronous `stopService` call for the non-target role to ensure clear forensic boundaries (Sep.07.70).

*(Total: 51 Architectural Rules + 239 Functional R-IDs = 290 Items)*
