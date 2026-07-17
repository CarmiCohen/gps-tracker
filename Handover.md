# Project Handover: July.17.00 - Performance Hardened Baseline

## 🔴 Status: HARDENED BASELINE ACHIEVED
**Version Context**: `July.17.00` (Authoritative)
**Target Hardware**: Samsung A15 (Budget Benchmark)
**Core Issue Resolved**: #526 (Startup Hang)

This document provides the definitive forensic state required to resume development in a new session. The system has been hardened against startup frame-spikes that previously impacted low-end hardware.

### 1. Forensic Status of Issue #526 (Resolved)
The "Landing Page Hang" was a race condition between background initialization and Main-thread property access during the cold-start window.
- **Root Cause**: `BaseMonitorService` and `MainViewModelFactory` triggered Room DB initialization on the Main thread via a "Lazy Cascade". Default Kotlin `lazy` synchronization caused kernel-level blocking.
- **Remediation**: 
    - **Lock-Free Lazy DI**: `AppContainer` properties now use `LazyThreadSafetyMode.PUBLICATION`. The Main thread will no longer block on background initializers.
    - **AppNotificationManager**: Decoupled from the repository layer. It now builds foreground notifications instantly without touching the database.
    - **Async Service Boot**: `BaseMonitorService.onCreate` is now logic-free. `startForeground` and hardware binding are deferred to a background scope.
    - **Warm-up**: `GpsApplication` proactively primes the DB and Managers on `Dispatchers.IO` immediately after creation.

### 2. Authoritative Data Models
- **`SystemHealthState`**: Authority for device metadata (Battery, Thermal, Storage, Signal).
- **`AlarmHistory`**: Authority for persistent violation counters (resident in-memory).
- **`HardwareCapabilities`**: Brand-agnostic abstraction of device restrictions.

### 3. Architectural Baselines
- **Manual Lazy DI**: Managed via `AppContainer.kt`. **STRICT RULE**: Never access a `container` property on the Main thread during the cold-start window (0-3s after launch).
- **Unified Heartbeat**: Global 2000ms standard (`TICK_INTERVAL_MS`).

### 4. Resumption Instructions
1. **Sync**: Perform a Gradle Sync immediately to regenerate `BuildConfig`.
2. **Build**: Run `:app:assembleDebug`.
3. **Verification**: Confirm version `July.17.00` appears on the landing page.
4. **Caution**: Any new global component added to `AppContainer` MUST be lazy and use `PUBLICATION` safety.

### 5. Future Simplification Ideas
- **Service Consolidation**: Merge `ViewerService` and `TrackerService` into a single role-configurable `MonitorService`.
- **Repository Flattening**: Consolidate `OfflineRepository` and `TelemetryRepository` into `MainRepository`.
