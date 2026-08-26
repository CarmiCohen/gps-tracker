# Architectural Simplification Ideas (vAug.26.07)

## 🎯 Current Objectives
- Reduce memory churn on budget hardware (A15).
- Simplify state propagation between Engine and App layers.
- Prevent diagnostic logging leaks in production.

## 💡 Ideas
1.  **Diagnostic Interceptor**: Centralize all forensic "StackLog" style traces into a single toggle-able interceptor within `IntegrityMonitor`. This would prevent direct `println` or manual stack-trace logging in providers like `SystemStatusProvider`.
2.  **Hardware Handshake**: Replace the "magic" 200ms settling delay in `onDestroy` with a deterministic handshake from the native `libjdHardware.so`.
3.  **Unified Grace Logic**: Combine `HARDWARE_BOOT_GRACE_MS` and `GPS_WARMUP_GRACE_MS` into a unified `StartupSettlingState`.
4.  **Stateless Violation Reporting**: Refactor `MainAlarmLogic` to return a stream of delta-violations.

## 🟢 Implemented Simplifications
- **Idle-Based Map Hydration**: (Aug.26.05) Migrated Map Engine init to Level 4 (IdleHandler) in `LifecycleHydrationManager` (Issue #323).
- **Deep Hardening Audit**: (Aug.26.04) Validated Anomaly Correlation Engine (R133) and Heat Mitigation (R191).
- **Native Cleanup Strategy**: (Aug.26.04) Hardened JNI destruction sequence (Issue #320).
- **Multi-Stage Hydration**: (Aug.26.02) Decomposed `TrackerScreen` into 3-stage hydration levels (Issue #321).
