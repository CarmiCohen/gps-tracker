# System Source of Truth (SoT) - v9.3.55 (July17.07)

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Database Migration Integrity (R956)**: Any change to an `@Entity` class MUST be accompanied by a version bump and an explicit `Migration` object. For tables with many columns (e.g., `logs`), a "Recreate-Copy-Rename" strategy is preferred over multiple `ALTER TABLE` calls to prevent schema drift and ensure consistency of default values. (v9.3.55 / Issue #096)
*   **Startup IO Offloading Authority (R955)**: All operations that trigger database opening or initial persistence loading (e.g., `loadInitialData`) MUST be executed on `Dispatchers.IO`. This prevents Room migrations or heavy disk reads from starving the Main thread during the cold start landing page sequence. (v9.3.55 / Issue #096)
*   **Landing Page Event Suppression (R954)**: To ensure state purity and minimize startup contention, the system MUST reject all frame notices and telemetry events while the Landing Page is active. (v9.3.52)
*   **Data Flow Offloading Authority (R953)**: To ensure UI responsiveness and prevent ANRs, all mapping operations from database entities to UI models MUST be offloaded to `Dispatchers.Default` using `.flowOn()` at the Repository level. (v9.3.52 / Issue #092)
*   **Reactive Setup Flow Authority (R952)**: The system MUST implement a reactive transition from the Landing Page to the operational mode. (v9.3.45 / Issue #095)
*   **Differential IPC Polling Authority (R951)**: To prevent main thread starvation (ANRs) on low-end hardware, the system MUST use differential polling for permissions. (v9.3.45 / Issue #095)
*   **Non-Blocking System API Authority (R406)**: The system MUST NOT execute system IPC calls using `runBlocking` or on the Main thread. (v9.3.30 / Issue #092)
*   **Automatic Mode Transition Delay (R925)**: The app MUST pause for two seconds (`LANDING_PAGE_PAUSE_MS`) on the landing page before automatically entering Tracker or Viewer mode. (Issue #092 / v9.3.36)
*   **Unified System Heartbeat (R403/R405)**: The system MUST use a standardized 2000ms heartbeat (`TICK_INTERVAL_MS`). (v9.3.20)
*   **Type Safety Authority (R999)**: All internal telemetry, sensor data, and engine pipelines MUST use `Double` precision. (Issue #077 / v9.3.15)
