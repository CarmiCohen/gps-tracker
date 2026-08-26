# Architectural Simplification Ideas (vAug.26.00)

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

## 🟢 Implemented Simplifications
- **LifecycleHydrationManager**: (Aug.26.00) Centralized staggered hydration sequence for budget hardware (Issue #318).
- **Native Retry Logic**: (Aug.26.00) Hardened `JdHardwareManager` with exponential backoff to resolve Monitor::Inflate failures (Issue #319).
- **Hardware SOT Object**: (Aug.25.05) Decoupled hardware detection from `:app` layer. Verified on SM-A155F.
- **Snap-Isolation Throttling**: (Aug.25.04) Reduced Compose recomposition cycles using parity-based filters.
- **Map Pool Isolation**: (Aug.25.00) Removed map overlays from Compose state to avoid lock verification overhead.
