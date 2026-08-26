# Architectural Simplification Ideas (vAug.26.05)

## 🎯 Current Objectives
- Reduce memory churn on budget hardware (A15).
- Simplify state propagation between Engine and App layers.
- Stabilize background monitor lifecycle.

## 💡 Ideas
1.  **Hardware Handshake**: Replace the "magic" 200ms settling delay in `onDestroy` with a deterministic handshake or callback from the native `libjdHardware.so` to signal that all event queues are disposed.
2.  **Unified Grace Logic**: Instead of separate `HARDWARE_BOOT_GRACE_MS` and `GPS_WARMUP_GRACE_MS`, consider a unified `StartupSettlingState`.
3.  **Stateless Violation Reporting**: Refactor `MainAlarmLogic` to return a stream of delta-violations.
4.  **Standardized LRU Provider**: Evaluate replacing the custom `ShadowCache` wrapper with `androidx.collection.LruCache`.

## 🟢 Implemented Simplifications
- **Idle-Based Map Hydration**: (Aug.26.05) Migrated Map Engine init to Level 4 (IdleHandler) in `LifecycleHydrationManager` to eliminate startup frame drops (Issue #323).
- **Deep Hardening Audit**: (Aug.26.04) Validated Anomaly Correlation Engine (R133) and Heat Mitigation (R191) under sustained 100Hz stress.
- **Native Cleanup Strategy**: (Aug.26.04) Hardened JNI destruction sequence to prevent BaseEventQueue leaks (Issue #320).
- **Multi-Stage Hydration**: (Aug.26.02) Decomposed `TrackerScreen` into 3-stage hydration levels to eliminate Davey stalls (Issue #321).
- **LifecycleHydrationManager**: (Aug.26.00) Centralized staggered hydration sequence.
- **Hardware SOT Object**: (Aug.25.05) Decoupled hardware detection from `:app` layer.
- **Snap-Isolation Throttling**: (Aug.25.04) Reduced Compose recomposition cycles using parity-based filters.
