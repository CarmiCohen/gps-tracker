# Architectural Simplification Ideas (vAug.26.12)

## 🎯 Current Objectives
- Reduce memory churn on budget hardware (A15).
- Simplify state propagation between Engine and App layers.
- Prevent diagnostic logging leaks in production.

## 💡 Ideas
1.  **Diagnostic Interceptor**: Centralize all forensic "StackLog" style traces into a single toggle-able interceptor within `IntegrityMonitor`. This would prevent direct `println` or manual stack-trace logging in providers like `SystemStatusProvider`.
2.  **Unified Grace Logic**: Combine `HARDWARE_BOOT_GRACE_MS` and `GPS_WARMUP_GRACE_MS` into a unified `StartupSettlingState`.
3.  **Stateless Violation Reporting**: Refactor `MainAlarmLogic` to return a stream of delta-violations.
4.  **IDS Persistence Audit**: Investigate if Identity Sanitization (IDS) training data can be moved to a simpler atomic preference store to prevent re-initialization noise (Concern #737).

## 🟢 Implemented Simplifications
- **Compilation Error Remediation**: (Aug.26.12) Simplified command routing by removing redundant `ClearTrails` event in `Models.kt` (Issue #736).
- **Setup Overlay Bypass**: (Aug.26.11) Implemented a developer-mode bypass for the `PhoneSetupOverlay` to allow automated testing (Issue #735).
- **Hardware Handshake**: (Aug.26.09) Replaced the 200ms "magic" settling delay in `onDestroy` with a deterministic native round-trip (`punchHardware`) (Issue #320).
- **Eager Internet Observation**: (Aug.26.08) Transitioned `SystemStatusProvider` to `SharingStarted.Eagerly` for internet status (Issue #723).
- **Idle-Based Map Hydration**: (Aug.26.05) Migrated Map Engine init to Level 4 (IdleHandler) in `LifecycleHydrationManager` (Issue #323).
