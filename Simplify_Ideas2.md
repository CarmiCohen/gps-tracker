# Simplicity Ideas 2 (Aug.26.19)

## 🏗️ Architectural Simplification
1.  **Unified Hardware Bootstrap**: Move `JdHardwareManager.initialize` from role-specific services (`TrackerService`) into `BaseMonitorService` or a dedicated `HardwareLifecycleManager`. This would eliminate the need for `isA15Device` checks and JNI init logic in the business-logic layer.
2.  **LocationCallback Factory**: Standardize all transient location requests (like revival pulses) through a factory that returns an `AutoCloseable` or `Job`-linked registration to prevent future anonymous listener leaks.
3.  **Monotonic Logic Consolidation**: Create a `MonotonicDuration` utility to wrap `elapsedRealtime` math, reducing the risk of wall-clock drift bugs in maintenance and silence detection logic.
