# SOT Master Requirements & Hardening Status (Sep.27.14)

## 🏗️ Architectural Master Rules (46 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.33 Reactive State Mapping Authority (R512)**: Reactive state mapping for dashboard, HUD, and map views must be extracted from ViewModels into a dedicated `UiStateCoordinator` authority to achieve thin ViewModels and isolate state projections (Issue #1350).
*   **1.34 Unified Subscription Scoping (R513)**: All reactive data stream collections within a component must be orchestrated within a single parent coroutine scope using child builders to ensure deterministic lifecycle management and reduce resource churn (Issue #1351).
*   **1.35 Lifecycle-Aware Tick Orchestration (R514)**: Background services must delegate periodic loop management and initialization state gates to a dedicated `TickOrchestrator` to ensure lifecycle consistency and simplify service execution logic (Issue #1293).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 514**: Lifecycle-Aware Tick Orchestration - Integrated a centralized `TickOrchestrator` to manage background service loops and initialization gates, eliminating decentralized `initializationDeferred` waits and fragmented job management. (Resolved Sep.27.14).
*   **SOT ID 513**: Unified StateSubscription Coroutine Scoping - Refactored MainViewModel to orchestrate all reactive flow collections under a single parent scope, eliminating redundant launch overhead and aligning with structured concurrency best practices. (Resolved Sep.27.13).
*   **SOT ID 512**: Unified State Mapping Authority - Extracted all reactive dashboard, HUD, and map state projection logic from `MainViewModel` into a dedicated `UiStateCoordinator` authority, achieving a perfectly thin ViewModel pattern. (Resolved Sep.27.12).
...

## 📋 Functional Requirements (176 R-IDs)
*   **R514**: Centralized `TickOrchestrator` for background lifecycle management.
*   **R513**: Unified StateSubscription coroutine scoping authority.
*   **R512**: Centralized `UiStateCoordinator` authority for reactive state mapping projections.
...

## 🏁 Verification Chapters
*   **Chapter 31.146 (Lifecycle-Aware Tick Orchestration)**: PASSED - Verified that MonitorService and BaseMonitorService now utilize TickOrchestrator for all periodic tasks and initialization gating. (Sep.27.14)
*   **Chapter 31.145 (Unified Subscription Scoping)**: PASSED - Verified that all 10+ data streams in MainViewModel are collected via a unified parent coroutine with child dispatchers. (Sep.27.13)
*   **Chapter 31.144 (Unified State Mapping Authority)**: PASSED - Successfully decoupled all reactive state projection and trail segments computation logic from MainViewModel into UiStateCoordinator. (Sep.27.12)
...
