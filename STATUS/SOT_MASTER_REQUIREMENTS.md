# SOT Master Requirements & Hardening Status (Sep.27.17)

## 🏗️ Architectural Master Rules (49 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.37 Protobuf-First Persistence (R516)**: Domain state persistence must prioritize structured Protobuf binary schemas over JSON strings to minimize serialization overhead, reduce GC pressure, and ensure strict schema evolution (Issue #1173).
*   **1.38 Zero-Allocation Telemetry Path (R517)**: High-frequency telemetry evaluation and propagation paths (ticks, GPS processing, signaling) must utilize pooled flyweight entities to eliminate GC churn and ensure architectural stability on budget hardware (Issue #1160).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 517**: Flyweight & Pooling Expansion - Refactored high-frequency telemetry DTOs (SystemEvaluationSnapshot, TrackerStatus, LocationUpdate) into reusable flyweights and optimized MonitorService/ConnectivitySuite to eliminate GC churn in the 1Hz evaluation loop. (Resolved Sep.27.17).
*   **SOT ID 516**: Protobuf-First Persistence - Replaced JSON-based alarm state storage with a pure Protobuf binary pipeline in DataStore, eliminating `org.json` overhead in the high-frequency evaluation path. (Resolved Sep.27.16).
*   **SOT ID 515**: Unified Service Job Orchestration - Extended `TickOrchestrator` to manage all lifecycle-bound jobs in `MonitorService`, ensuring atomic cleanup and unified initialization gating for all background tasks. (Resolved Sep.27.15).
*   **SOT ID 514**: Lifecycle-Aware Tick Orchestration - Integrated a centralized `TickOrchestrator` to manage background service loops and initialization gates, eliminating decentralized `initializationDeferred` waits and fragmented job management. (Resolved Sep.27.14).
...

## 📋 Functional Requirements (179 R-IDs)
*   **R517**: Flyweight & Pooling Expansion for zero-allocation telemetry.
*   **R516**: Protobuf-First Persistence for alarm and logic state.
*   **R515**: Unified `TickOrchestrator` management for all background lifecycle-bound jobs.
*   **R514**: Centralized `TickOrchestrator` for background lifecycle management.
...

## 🏁 Verification Chapters
*   **Chapter 31.149 (Flyweight & Pooling Expansion)**: PASSED - Verified that MonitorService, ConnectivitySuite, and AppEventCoordinator now utilize reusable flyweight instances for 1Hz evaluation and signaling, with zero object allocations in the steady-state path. (Sep.27.17)
*   **Chapter 31.148 (Protobuf-First Persistence)**: PASSED - Verified that AppAlarmManager and SettingsRepository now utilize ActiveAlarmProto for state persistence, with zero JSONObject/JSONArray allocations. (Sep.27.16)
*   **Chapter 31.147 (Unified Service Job Orchestration)**: PASSED - Verified that all background jobs in MonitorService (GPS, GNSS, Alarm Eval, Observers) are now orchestrated via TickOrchestrator. (Sep.26.11)
...
