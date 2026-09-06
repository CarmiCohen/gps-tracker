# Simplification Ideas (Sep.05.30)

## 1. HardwareProvider Refactoring
*   **Decouple Auditing**: Move GNSS jitter monitoring, sensor rate audits (R-ID 256), and energy footprint snapshots (R-ID 259) into a separate `ForensicAuditor` component. `HardwareProvider` should focus on raw data acquisition, while the auditor handles performance verification and power cost quantification.
*   **Snapshot Lifecycle**: Centralize the Forensic/Logic snapshot pooling into a generic `CircularStateBuffer` to reduce boilerplate in `HardwareProvider`.

## 2. Event Dispatching
*   **Unified SharedFlow**: Consider using a single `SystemEvent` stream instead of separate `revivalEvents`, `sensorEvents`, and `locationStatusFlow` to simplify downstream consumption in `MainViewModel`.

## 3. Battery State Unification
*   **Synchronous/Reactive Hybrid**: `SystemStatusProvider` now maintains both a `BatteryStatus` flow and a synchronous `getBatteryStatus()` method. These should be unified so the flow always reflects the latest high-resolution snapshot captured during audits, preventing duplicate `Intent` registration overhead.
