# System Source of Truth (SoT) - v9.3.52 (July17.06)

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Data Flow Offloading Authority (R953)**: To ensure UI responsiveness and prevent ANRs, all mapping operations from database entities to UI models (Logs, Trails, History) MUST be offloaded to `Dispatchers.Default` using `.flowOn()` at the Repository level. No heavy list processing or JSON parsing is permitted on the Main thread. (v9.3.52 / Issue #092)
*   **Reactive Setup Flow Authority (R952)**: The system MUST implement a reactive transition from the Landing Page to the operational mode. (v9.3.45 / Issue #095)
*   **Differential IPC Polling Authority (R951)**: To prevent main thread starvation (ANRs) on low-end hardware, the system MUST use differential polling for permissions. (v9.3.45 / Issue #095)
*   **Non-Blocking System API Authority (R406)**: The system MUST NOT execute system IPC calls (permissions, battery optimizations, overlays) using `runBlocking` or on the Main thread. (v9.3.30 / Issue #092)
*   **Automatic Mode Transition Delay (R925)**: The app MUST pause for two seconds (`LANDING_PAGE_PAUSE_MS`) on the landing page before automatically entering Tracker or Viewer mode. (Issue #092 / v9.3.36)
*   **Samsung A15 Hardening Authority (R405)**: The system MUST prioritize background resilience on Samsung A15 devices. (v9.3.25)
*   **Unified System Heartbeat (R403/R405)**: The system MUST use a standardized 2000ms heartbeat (`TICK_INTERVAL_MS`). (v9.3.20)
*   **Type Safety Authority (R999)**: All internal telemetry, sensor data, and engine pipelines MUST use `Double` precision. (Issue #077 / v9.3.15)

... [Rest of document remains unchanged]
