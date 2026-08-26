# Architectural Simplification Ideas (vAug.26.13)

## 🎯 Current Objectives
- Reduce memory churn on budget hardware (A15).
- Simplify state propagation between Engine and App layers.
- Prevent diagnostic logging leaks in production.

## 💡 Ideas
1.  **Diagnostic Interceptor**: Centralize all forensic "StackLog" style traces into a single toggle-able interceptor within `IntegrityMonitor`. This would prevent direct `println` or manual stack-trace logging in providers like `SystemStatusProvider`.
2.  **Unified Grace Logic**: Combine `HARDWARE_BOOT_GRACE_MS` and `GPS_WARMUP_GRACE_MS` into a unified `StartupSettlingState`.
3.  **Stateless Violation Reporting**: Refactor `MainAlarmLogic` to return a stream of delta-violations.

## 🟢 Implemented Simplifications
- **IDS Persistence Hardening**: (Aug.26.13) Resolved Concern #737 by persisting the dismissal of identity sanitization warnings, eliminating redundant re-initialization noise.
- **Compilation Error Remediation**: (Aug.26.12) Simplified command routing by removing redundant `ClearTrails` event in `Models.kt` (Issue #736).
- **Setup Overlay Bypass**: (Aug.26.11) Implemented a developer-mode bypass for the `PhoneSetupOverlay` (Issue #735).
- **Hardware Handshake**: (Aug.26.09) Replaced the 200ms "magic" settling delay in `onDestroy` with a deterministic native round-trip (`punchHardware`) (Issue #320).
- **Eager Internet Observation**: (Aug.26.08) Transitioned `SystemStatusProvider` to `SharingStarted.Eagerly` for internet status (Issue #723).
