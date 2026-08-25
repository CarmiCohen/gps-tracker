# Architectural Simplification Ideas (vAug.25.01)

## 🎯 Current Objectives
- Reduce memory churn on budget hardware (A15).
- Simplify state propagation between Engine and App layers.

## 💡 Ideas
1.  **Unified Grace Logic**: Instead of separate `HARDWARE_BOOT_GRACE_MS` and `GPS_WARMUP_GRACE_MS`, consider a unified `StartupSettlingState` in `AlarmEvaluationState` that encapsulates all time-based suppressions.
2.  **Stateless Violation Reporting**: Refactor `MainAlarmLogic` to return a stream of delta-violations rather than a full report each tick, reducing the allocation of `ViolationReport` objects.
3.  **Monotonic-Only Evaluation**: Remove all `currentTimeMillis` dependencies from `MainAlarmLogic` to eliminate risk of clock-skew false positives.
4.  **Baseline Quantization**: Store EMA baselines (Baro/Acoustic) in a separate `EnvironmentContext` to decouple environmental tracking from alarm evaluation logic.
