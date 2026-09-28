# SOT Master Requirements & Hardening Status (Sep.28.3)

## 🏗️ Architectural Master Rules (52 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.37 Protobuf-First Persistence (R516)**: Domain state persistence must prioritize structured Protobuf binary schemas over JSON strings to minimize serialization overhead, reduce GC pressure, and ensure strict schema evolution (Issue #1173).
*   **1.38 Zero-Allocation Telemetry Path (R517)**: High-frequency telemetry evaluation and propagation paths (ticks, GPS processing, signaling) must utilize pooled flyweight entities to eliminate GC churn and ensure architectural stability on budget hardware (Issue #1160).
*   **1.39 Activity-Aware Power Scaling (R518)**: GPS and sensor polling intervals must dynamically scale based on user activity context (STILL, WALKING, IN_VEHICLE) to maximize battery life while ensuring coordinate precision during high-velocity movement (Issue #1205).
*   **1.40 Isolated Activity Context Provider (R519)**: Activity Recognition and low-level heuristic fallback state logic must be entirely isolated inside a dedicated context provider to keep the hardware management layer decoupled and modular (Issue #1353).
*   **1.41 Build-Time Interface Validation (R520)**: All core domain and service interfaces must be verified for Hilt binding consistency at build-time to prevent runtime ProvisionException errors and ensure cross-module architectural integrity (Issue #1294).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 520**: Build-Time Interface Validation - Implemented a custom Gradle task to perform static analysis of Hilt modules, ensuring all required core engine interfaces are properly bound before compilation. (Resolved Sep.28.3).
*   **SOT ID 519**: Unified Activity Context Provider - Consolidated Google Play Services Activity Recognition and GPS heuristic fallbacks into a standalone provider, offloading high-level tracking logic from the hardware suite. (Resolved Sep.28.2).
*   **SOT ID 518**: Context-Aware Power Optimization - Fully integrated Google Activity Recognition API and heuristic fallbacks into the tracking engine, allowing immediate interval relaxation when STILL and maintaining precision when IN_VEHICLE. (Resolved Sep.28.1).
*   **SOT ID 517**: Flyweight & Pooling Expansion - Refactored high-frequency telemetry DTOs (SystemEvaluationSnapshot, TrackerStatus, LocationUpdate) into reusable flyweights and optimized MonitorService/ConnectivitySuite to eliminate GC churn in the 1Hz evaluation loop. (Resolved Sep.27.17).
*   **SOT ID 516**: Protobuf-First Persistence - Replaced JSON-based alarm state storage with a pure Protobuf binary pipeline in DataStore, eliminating `org.json` overhead in the high-frequency evaluation path. (Resolved Sep.27.16).
...

## 📋 Functional Requirements (182 R-IDs)
*   **R520**: Static verification of Hilt bindings for core interfaces.
*   **R519**: Modular encapsulation of user activity context tracking.
*   **R518**: Activity-aware GPS interval scaling logic.
*   **R517**: Flyweight & Pooling Expansion for zero-allocation telemetry.
*   **R516**: Protobuf-First Persistence for alarm and logic state.
...

## 🏁 Verification Chapters
*   **Chapter 31.152 (Build-Time Interface Validation)**: PASSED - Verified that the `verifyInterfaceBindings` task correctly identifies missing Hilt bindings and fails the build as expected. (Sep.28.3)
*   **Chapter 31.151 (Unified Activity Context Provider)**: PASSED - Verified standalone operation of ActivityContextProvider, including seamless heuristic fallback and broadcast extraction. (Sep.28.2)
*   **Chapter 31.149 (Flyweight & Pooling Expansion)**: PASSED - Verified that MonitorService, ConnectivitySuite, and AppEventCoordinator now utilize reusable flyweight instances for 1Hz evaluation and signaling, with zero object allocations in the steady-state path. (Sep.28.3)
...
