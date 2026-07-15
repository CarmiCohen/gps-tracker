# System Source of Truth (SoT) - v9.4.0 (Development)

This document serves as the definitive operational specification for the GPS-Tracker system. All Issue IDs referenced here are Authoritative.

### 1. Core Architectural Baselines
*   **Unified System Heartbeat (R406a / Issue #501)**: The system MUST use a standardized 2000ms heartbeat (`TICK_INTERVAL_MS`) globally for all hardware polling, logic cycles, and telemetry submissions. ALL variable polling intervals (Moving, Stationary, Suspicious, High-Frequency) are DEPRECATED and REMOVED. This simplifies the state machine and ensures predictable data density. (v9.4.0)
*   **Binary Telemetry Authority (R988)**: The system MUST prioritize binary Protobuf-based telemetry (`location_update_bin`) for high-frequency tracker updates to minimize relay bandwidth and improve device performance. The schema MUST use optimized Enums for states and reasons. (v9.3.25)
*   **Alias-Aware Identity Uniqueness (R182b)**: The system MUST enforce identity uniqueness that accounts for reserved legacy aliases (`T`, `V`, `Trk`, `viewer`). Tracker and Viewer IDs MUST NOT conflict with these reserved sets. (v9.3.25)
*   **Samsung A15 Hardening Authority (R405)**: The system MUST prioritize background persistence on Samsung A15 devices through combined mechanisms: (1) `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, (2) Unified 2s heartbeat, (3) `TYPE_STEP_DETECTOR` stay-alive sensor, and (4) Accelerometer-based fallback. (v9.3.25)
*   **Relay Configuration Authority (R404)**: The system MUST enforce a centralized relay configuration via `MainRepository.DEFAULT_RELAY_URL`. (v9.3.18)
*   **Automatic Mode Transition Delay (R925)**: The app MUST pause for 2s (`LANDING_PAGE_PAUSE_MS`) on the landing page for automatic restoration. Manual selection MUST be immediate. (Issue #092 / v9.3.36)
*   **System API Synchronization Authority (R999b)**: The background service layer MUST maintain strict signature parity with the `:core:engine` telemetry pipeline. (v9.3.16)
*   **Map Follow Mode Persistence (R981b)**: The map system MUST respect the user's manual focus intent (Tracker, Viewer, or Auto). (v9.3.16)
*   **Type Safety Authority (R999)**: All internal telemetry, sensor data, and engine pipelines MUST use `Double` precision. (v9.3.15)
*   **System API Throttling (R998)**: The system MUST enforce a minimum 10,000ms TTL cache for all system API permission and status checks to eliminate logcat jitter. (v9.3.14)
*   **Dynamic Anchor Breakout (R990c)**: The engine MUST implement a displacement-weighted monitor to prevent "sticky anchors". (v9.3.13)
*   **Forensic Visual Authority (R404b)**: The system MUST use a standardized `FORENSIC_PINK_COLOR` (#FF1493) for all forensic events. (v9.3.18)
