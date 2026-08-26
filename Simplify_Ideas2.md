# Architectural Simplification Ideas (vAug.26.10)

## 🎯 Current Objectives
- Reduce memory churn on budget hardware (A15).
- Simplify state propagation between Engine and App layers.
- Prevent diagnostic logging leaks in production.

## 💡 Ideas
1.  **Diagnostic Interceptor**: Centralize all forensic "StackLog" style traces into a single toggle-able interceptor within `IntegrityMonitor`. This would prevent direct `println` or manual stack-trace logging in providers like `SystemStatusProvider`.
2.  **Unified Grace Logic**: Combine `HARDWARE_BOOT_GRACE_MS` and `GPS_WARMUP_GRACE_MS` into a unified `StartupSettlingState`.
3.  **Stateless Violation Reporting**: Refactor `MainAlarmLogic` to return a stream of delta-violations.
4.  **Setup Overlay Bypass**: Implement a developer-mode bypass for the `PhoneSetupOverlay` to allow automated testing and soak tests on headless or remote devices without manual permission configuration (Issue #735).

## 🟢 Implemented Simplifications
- **Hardware Handshake**: (Aug.26.09) Replaced the 200ms "magic" settling delay in `onDestroy` with a deterministic native round-trip (`punchHardware`). This ensures the native event queue is drained and the JNI bridge is responsive before release (Issue #320).
- **Eager Internet Observation**: (Aug.26.08) Transitioned `SystemStatusProvider` to `SharingStarted.Eagerly` for internet status. This simplifies the lifecycle by ensuring the `ConnectivityManager` callback is registered once, eliminating the complexity and platform noise of on-demand re-registration (Issue #723).
- **Idle-Based Map Hydration**: (Aug.26.05) Migrated Map Engine init to Level 4 (IdleHandler) in `LifecycleHydrationManager` (Issue #323).
- **Deep Hardening Audit**: (Aug.26.04) Validated Anomaly Correlation Engine (R133) and Heat Mitigation (R191).
- **Native Cleanup Strategy**: (Aug.26.04) Hardened JNI destruction sequence (Issue #320).
- **Multi-Stage Hydration**: (Aug.26.02) Decomposed `TrackerScreen` into 3-stage hydration levels (Issue #321).
