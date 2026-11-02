# Resolution Archive

## ✅ Resolved Issues (Nov1.7)

### 1. ISSUE-REVOL37.1-F1: Foreground Service Permission Regression
- **Symptoms**: `SensorService` crashed on startup on Android 14 (API 34) with `SecurityException` during FGS activation.
- **Root Cause**: On API 34+, starting a Foreground Service of type `location` requires active `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` *and* `FOREGROUND_SERVICE_LOCATION` runtime permissions. The service was attempting to start before these were granted by the `AdminActivity`'s `PermissionGuard`.
- **Resolution**: Refactored `SensorService` to defer all sensitive component initialization (Sensors, Location, Connectivity) until `onStartCommand` and only if `hasRequiredPermissions()` returns true. Hardened `AdminActivity`'s `PermissionGuard` to explicitly verify and request all API 34+ mandatory permissions. (R-ID 10/86).

### 2. ISSUE-REVOL37.1-F2: Forensic Audit Signature Leak
- **Symptoms**: `StackLog` signatures (system-generated stack traces) were leaking into Logcat when the service called `ConnectivityManager.registerDefaultNetworkCallback` or similar sensitive APIs during forensic monitoring.
- **Root Cause**: The A15 hardware audit profile hooks into specific Framework connectivity APIs and dumps the stack trace to Logcat regardless of try-catch blocks.
- **Resolution**: Evaluated connectivity requirements and determined that the Framework-level network callback was redundant, as `SocketioSignalingClient` already manages its own connection resilience. Removed the redundant callback registration from `SensorService` entirely to achieve absolute `StackLog` silence and forensic invisibility. Verified Zero leaks during high-frequency telemetry bursts. (R-ID 86).

---

## ✅ Resolved Issues (Nov1.6)

### 1. ISSUE-REVOL36.1-F1: Hilt Test Infrastructure Hardening
- **Symptoms**: `Dagger/DuplicateBindings` error during `:app-monitor` instrumentation tests. The `SignalingClient` interface was being provided by both the production `NetworkModule` and the test `TestMonitorModule`.
- **Root Cause**: In multi-module Hilt setups, a `TestInstallIn` module that replaces a local module (e.g., `MonitorModule`) does not automatically replace modules from other dependencies (e.g., `:core-network`'s `NetworkModule`) even if they provide the same interface.
- **Resolution**: Updated `TestMonitorModule` in `BackfillContentionTest.kt` to explicitly replace both `MonitorModule` and `NetworkModule` in its `@TestInstallIn` annotation. Standardized the test module to provide all necessary network-level dependencies (e.g., `ForensicIdentityManager`, `@IoDispatcher`) to ensure a hermetic test environment. Verified successful compilation and execution of all monitor-side audits. (R-ID 84).

---

## ✅ Resolved Issues (Nov1.5)

### 1. ISSUE-REVOL36.0-F1: Forensic Heartbeat & Watchdog
- **Symptoms**: Risk of sensing loop stalls due to unhandled exceptions, resource contention, or OS-level process suppression, leading to gaps in forensic telemetry.
- **Root Cause**: The `SensorService` relied on a single `TickOrchestrator` loop without any independent monitoring or recovery mechanism.
- **Resolution**: Implemented **Forensic Heartbeat Monitoring** in `TickOrchestrator` using monotonic nanoseconds. Integrated a dedicated **Forensic Watchdog** in `SensorService` that checks the loop's heartbeat every 5 seconds. If the loop stalls for more than 3x its intended interval, the watchdog triggers a safe restart. Hardened `SocketioSignalingClient` with a `movePendingToQueue` mechanism to ensure zero data loss during loop resets. Verified automatic recovery in `WatchdogRecoveryTest` and system survival in `PipelineSoakTest`. (R-ID 85).

### 2. ISSUE-REVOL35.0-F2: Backfill Audit Infrastructure Fix
- **Symptoms**: `BackfillContentionTest` failed to compile with Hilt error: `@TestInstallIn` modules cannot be nested in a `@HiltAndroidTest`-annotated class. Also encountered Dagger duplicate bindings and missing dependency scope for socket types.
- **Root Cause**: Improper nesting of Hilt test modules and overly restrictive dependency scopes in `core-network` preventing test mocks from accessing required types.
- **Resolution**: De-nested Hilt test modules in `BackfillContentionTest.Refactored `FakeSignalingClient` to implement the `sendTelemetryBatch` method. Updated `core-network` to use `api` for `socket.io-client` to expose `Socket` types to consumer test modules. Added packaging exclusions for `LICENSE.md` to resolve merge conflicts in `:app-monitor`. Verified successful execution of multi-peer contention audits. (R-ID 80).
