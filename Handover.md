# Project Handover: R406 Hardened Baseline - July.16.22

## 🔴 Status: HARDENED BASELINE ACHIEVED
**Version Context**: `July.16.22`
**Authoritative Specifications**: `STATUS/SOT_MASTER_REQUIREMENTS.md`

This document provides the forensic state required to resume development in a new session. All R406 series simplification and hardening tasks are complete, including remediation of post-migration regressions and performance bottlenecks.

### 1. Authoritative Data Models (The Core Engine)
- **`SystemHealthState`**: Sole source of truth for device metadata (Battery, Thermal, Storage, Signal). Produced by `IntegrityMonitor`, propagated via `ConnectivitySuite`.
- **`AlarmHistory`**: Stateless persistent metadata (violation counters, first-trigger timestamps) used by `MainAlarmLogic`. Managed by `AppAlarmManager` but resident in memory within the engine.
- **`HardwareCapabilities`**: Brand-agnostic abstraction of device-specific restrictions (Samsung wake-locks, Xiaomi autostart).

### 2. Architectural Forensics
- **Manual DI**: Hilt/Dagger completely purged. Dependencies managed via singleton `AppContainer` in `GpsApplication`.
- **ConnectivitySuite**: Unified component handling Socket.io, HTTP Keep-alive, and Telemetry synchronization. It maintains the remote peer state.
- **Unified Heartbeat**: Enforced 2000ms standard (`TICK_INTERVAL_MS`) for all logic cycles and hardware polling.

### 3. Recent Hardening Remediation (Issues #518 - #526)
The following critical regressions and performance issues identified after the R406 migration have been resolved:
- **Issue #526: A15 Landing Page Hang**: Offloaded `ConnectivitySuite` initialization and internal loops to `Dispatchers.Default` and `Dispatchers.IO`. This prevents Main thread contention during cold start, resolving ANR reports on budget devices like Samsung A15.
- **Viewer Monitoring Parity**: Fixed `ViewerService` mapping errors. Viewers now correctly monitor the **remote Tracker's** health (Cooling, Storage, Power Save) rather than their own local phone flags.
- **Stable Stationary Anchor (R406m)**: `LocationProcessor` now locks a geographic anchor when speed < 0.5m/s, eliminating coordinate wander and jitter during parking.
- **Passive Tilt Zeroing (R406l)**: `LocationSentinel` adopts a relative tilt baseline after 5 minutes of stability, allowing for tilt alarms on uneven surfaces.
- **Thread Safety**: Synchronized access to `IntegrityMonitor.currentHealth` using a dedicated lock.
- **Acoustic Fast-Path**: Hardware acoustic spikes now trigger a 1s lockout in the processing engine to prevent alarm spam.

### 4. Current File-System State
- **`:core:engine`**: Stateless and brand-agnostic. Contains `MainAlarmLogic`, `LocationSentinel`, and `PhysicsUtils`.
- **`TrackerService.kt` / `ViewerService.kt`**: Thin background coordinators.
- **`ConnectivitySuite.kt`**: Single point of entry for all network and peer logic. Now fully asynchronous.
- **`IntegrityMonitor.kt`**: Sole producer of hardware health metadata.

### 5. Resumption Instructions
1. **Build**: Run `:app:assembleDebug` to verify baseline integrity.
2. **Context**: If starting in a fresh chat, index `SOT_MASTER_REQUIREMENTS.md` and `AppContainer.kt` first to understand the dependency graph.
3. **Next Phase**: The system is ready for UI refinements or high-level feature additions. No further core "simplification" of the engine is required at this time.
