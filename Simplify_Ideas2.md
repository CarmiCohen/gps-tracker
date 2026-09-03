# Simplification & Optimization Ideas (Sep.03.120)

## 1. Service Consolidation
*   **1.1. Location Processing Redundancy**: `TrackerService` and `ViewerService` share ~80% of their logic for observing `LocationProcessor` and `HardwareProvider` flows. This event delegation should be hoisted into `BaseMonitorService` to reduce forensic snapshot duplication and ensure logic parity across roles.
*   **1.2. Command Routing**: Consolidate `commandRouter` observation into the base class to standardize the handling of `UiVisibilityChanged` and `SyncSensors` events.

## 2. Telemetry Optimization
*   **2.1. Snapshot Flyweights**: The `ForensicSnapshot` and `TrackerStatus` mapping logic in `ConnectivitySuite` can be further optimized using pre-allocated byte-buffer flyweights to eliminate the overhead of intermediate object creation during high-frequency GPS pulses.

## 3. Lifecycle Hydration
*   **3.1. Unified State Flow**: Migrate the remaining discrete flows in `MainViewModel` (RTT, Signal, CurrentMa) into the segmented `DashboardState` or `HudState` to reduce the number of individual collectors active in the UI layer.

---
**Audit Identification: Issue #899**
