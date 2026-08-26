# Architectural Simplification Ideas (vAug.25.06)

## 🎯 Current Objectives
- Reduce memory churn on budget hardware (A15).
- Simplify state propagation between Engine and App layers.
- Stabilize background monitor lifecycle.

## 💡 Ideas
1.  **Unified Grace Logic**: Instead of separate `HARDWARE_BOOT_GRACE_MS` and `GPS_WARMUP_GRACE_MS`, consider a unified `StartupSettlingState` in `AlarmEvaluationState` that encapsulates all time-based suppressions.
2.  **Stateless Violation Reporting**: Refactor `MainAlarmLogic` to return a stream of delta-violations rather than a full report each tick, reducing the allocation of `ViolationReport` objects.
3.  **Monotonic-Only Evaluation**: Remove all `currentTimeMillis` dependencies from `MainAlarmLogic` to eliminate risk of clock-skew false positives.
4.  **Baseline Quantization**: Store EMA baselines (Baro/Acoustic) in a separate `EnvironmentContext` to decouple environmental tracking from alarm evaluation logic.
5.  **Standardized LRU Provider**: Evaluate replacing the custom `ShadowCache` wrapper with `androidx.collection.LruCache` to reduce custom concurrency boilerplate.
6.  **LifecycleHydrationManager**: (New) Staggered hydration (R314) is currently scattered in ViewModels. A dedicated manager could centralize the staggered launch sequence for budget hardware (Issue #318).
7.  **Component Composition for Services**: (New) Simplify `TrackerService` and `ViewerService` by delegating monitor setup to a shared `MonitorLifecycleDelegate` to address Issue #319.

## 🟢 Implemented Simplifications
- **Hardware SOT Object**: (Aug.25.05) Decoupled hardware detection from `:app` layer. Verified on SM-A155F.
- **Snap-Isolation Throttling**: (Aug.25.04) Reduced Compose recomposition cycles using parity-based filters.
- **Map Pool Isolation**: (Aug.25.00) Removed map overlays from Compose state to avoid lock verification overhead.
