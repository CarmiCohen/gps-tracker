# SOT Master Requirements & Hardening Status (Sep.27.16)

## 🏗️ Architectural Master Rules (48 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.33 Reactive State Mapping Authority (R512)**: Reactive state mapping for dashboard, HUD, and map views must be extracted from ViewModels into a dedicated `UiStateCoordinator` authority to achieve thin ViewModels and isolate state projections (Issue #1350).
*   **1.34 Unified Subscription Scoping (R513)**: All reactive data stream collections within a component must be orchestrated within a single parent coroutine scope using child builders to ensure deterministic lifecycle management and reduce resource churn (Issue #1351).
*   **1.35 Lifecycle-Aware Tick Orchestration (R514)**: Background services must delegate periodic loop management and initialization state gates to a dedicated `TickOrchestrator` to ensure lifecycle consistency and simplify service execution logic (Issue #1293).
*   **1.36 Unified Service Job Orchestration (R515)**: All background lifecycle-bound jobs (GPS collection, settings observation, FGS updates) must be managed by the `TickOrchestrator` to ensure atomic cleanup and consistent initialization gating, eliminating fragmented manual job management (Issue #1352).
*   **1.37 Protobuf-First Persistence (R516)**: Domain state persistence must prioritize structured Protobuf binary schemas over JSON strings to minimize serialization overhead, reduce GC pressure, and ensure strict schema evolution (Issue #1173).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 516**: Protobuf-First Persistence - Replaced JSON-based alarm state storage with a pure Protobuf binary pipeline in DataStore, eliminating `org.json` overhead in the high-frequency evaluation path. (Resolved Sep.27.16).
*   **SOT ID 515**: Unified Service Job Orchestration - Extended `TickOrchestrator` to manage all lifecycle-bound jobs in `MonitorService`, ensuring atomic cleanup and unified initialization gating for all background tasks. (Resolved Sep.27.15).
*   **SOT ID 514**: Lifecycle-Aware Tick Orchestration - Integrated a centralized `TickOrchestrator` to manage background service loops and initialization gates, eliminating decentralized `initializationDeferred` waits and fragmented job management. (Resolved Sep.27.14).
...

## 📋 Functional Requirements (178 R-IDs)
*   **R516**: Protobuf-First Persistence for alarm and logic state.
*   **R515**: Unified `TickOrchestrator` management for all background lifecycle-bound jobs.
*   **R514**: Centralized `TickOrchestrator` for background lifecycle management.
...

## 🏁 Verification Chapters
*   **Chapter 31.148 (Protobuf-First Persistence)**: PASSED - Verified that AppAlarmManager and SettingsRepository now utilize ActiveAlarmProto for state persistence, with zero JSONObject/JSONArray allocations. (Sep.27.16)
*   **Chapter 31.147 (Unified Service Job Orchestration)**: PASSED - Verified that all background jobs in MonitorService (GPS, GNSS, Alarm Eval, Observers) are now orchestrated via TickOrchestrator. (Sep.26.11)
...
