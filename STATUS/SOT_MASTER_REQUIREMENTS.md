# SOT Master Requirements (Aug.28.07)

This document serves as the Source of Truth (SOT) for the application's architectural rules and functional requirements.

## 🏗️ Architectural Master Rules (21 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context Isolation**: Components must use `@ApplicationContext` to avoid Activity-leak scenarios (R110).
*   **1.2 Deterministic Cleanup**: Services must explicitly cancel all jobs and unregister hardware listeners in `onDestroy` (R112).
*   **1.3 Atomic State Management**: All shared state must be managed via thread-safe primitives (AtomicBoolean, Mutex) or StateFlow (R113).
*   **1.4 Background Resilience**: Foreground services must be strictly managed with appropriate types and notifications to prevent OS-level killing (R114).
*   **1.5 Hardened IO**: All file and database operations must be offloaded from the Main thread and use transactional integrity (R115).
*   **1.6 Monotonic Time**: Use `elapsedRealtime` for all interval and duration logic to survive clock regressions and drift (R116).
*   **1.7 Single Source of Truth**: All system state (Health, Location, Alarms) must be centralized in repositories and propagated via Flows (R117).
*   **1.8 Lifecycle Synchronization (R738/R742/R744/R745/R746/R747/R748/R749/R750/R752/R753/R754/R755/R756)**: **MANDATORY**. Hardware listeners (GPS, Sensors, Network, GNSS) must use `ManagedHardware` abstractions for synchronous, trace-logged unregistration. Failure to unregister before thread disposal or service destruction is a critical architectural violation. (Updated Aug.28.07).

... [Remainder of rules preserved] ...

## 🧬 Change History (Recent)
*   **Aug.28.07**: Resolved Concern #756 (Persistent GNSS/Network Leak). Hardened `ManagedHardware` with fallback unregistration paths and added explicit trace logging to `GpsManager` and `CommunicationManager` to silence `BaseEventQueue` warnings (R756).
*   **Aug.28.06**: Resolved Concern #755 (GNSS & Network Unregistration Hardening). Standardized GNSS unregistration by implementing `ManagedGnssStatusCallback` in `ManagedHardware.kt`.
*   **Aug.28.05**: Resolved Concern #754 (Managed Sensor Abstraction). Introduced `ManagedSensorListener` and `ManagedDisplayListener` to standardize synchronous hardware unregistration.
*   **Aug.28.03**: Resolved Concern #752 (Persistent BaseEventQueue Leak). Remediated deadlock in `ManagedHardware.unregister` by checking Main Looper context.
