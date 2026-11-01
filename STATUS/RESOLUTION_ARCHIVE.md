# Resolution Archive

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
- **Resolution**: De-nested Hilt test modules in `BackfillContentionTest`. Refactored `FakeSignalingClient` to implement the `sendTelemetryBatch` method. Updated `core-network` to use `api` for `socket.io-client` to expose `Socket` types to consumer test modules. Added packaging exclusions for `LICENSE.md` to resolve merge conflicts in `:app-monitor`. Verified successful execution of multi-peer contention audits. (R-ID 80).

---

## ✅ Resolved Issues (Nov1.4)

### 1. ISSUE-REVOL35.0-F1: Battery-Aware Thermal Throttling
- **Symptoms**: Risk of device shutdown or permanent battery degradation during high-performance tracking (10Hz) under extreme heat or critical low power.
- **Root Cause**: The sensing loop lacked hardware-aware constraints, prioritizing tracking fidelity over device survival.
- **Resolution**: Implemented **Multi-Stage Resource Throttling** in `SensingEngine`. Telemetry intervals are now capped based on hardware health: 1Hz during Thermal Warning (>40°C) or Low Battery (<20%), and 0.1Hz during Critical Thermal (>45°C) or Critical Battery (<15%). Integrated `BatteryReceiver` and `OnThermalStatusChangedListener` in `SensorService` to trigger immediate sampling adaptation via loop preemption. Verified priority hierarchy (Thermal > Battery > Velocity) in `ThermalThrottlingTest`. (R-ID 83).

---

## ✅ Resolved Issues (Nov1.3)

### 1. ISSUE-REVOL34.0-F1: Adaptive Sampling Logic
- **Symptoms**: High battery consumption due to constant 10Hz telemetry sampling even when the device is stationary or moving slowly.
- **Root Cause**: The sensing loop in `SensorService` was hardcoded to a high frequency (10Hz) regardless of the physical movement state of the device.
- **Resolution**: Implemented **Velocity-Based Adaptive Sampling**. Integrated `FusedLocationProviderClient` in `SensorService` to monitor real-time speed. Updated `SensingEngine.getOptimalIntervalMs` to return dynamic intervals: 10Hz (>5m/s), 1Hz (>0.5m/s), and 0.1Hz (stationary). Utilized `TickOrchestrator.preemptLoop` to force the sensing loop to adapt immediately when a velocity-driven interval change occurs. Verified transition logic and battery survival floors in `AdaptiveSamplingTest`. (R-ID 82).

---

## ✅ Resolved Issues (Nov1.2)

### 1. ISSUE-REVOL33.0-F1: Forensic Signaling Batching
- **Symptoms**: High network packet overhead and potential throughput bottlenecks during high-frequency telemetry bursts (10Hz) or backfill recovery. Individual JSON payloads for every location update increase header overhead and battery consumption.
- **Root Cause**: `SocketioSignalingClient` was sending each telemetry update as a standalone socket.io message. While reliable, this approach lacked efficiency during sustained data flows.
- **Resolution**: Implemented **Adaptive Forensic Batching** in `SocketioSignalingClient`. Telemetry updates are now buffered in a `Mutex`-protected list and flushed based on a 500ms timer or a 10-item limit. Crucially, **Critical Event Protection** was integrated: any update with a non-standard status (e.g., "JUMP", geofence transitions) or any `ViolationEvent` triggers an immediate flush of the pending batch to ensure real-time responsiveness. Verified batching efficiency and priority handling in `SignalingBatchTest` and integrated reliability in `PipelineSoakTest`. (R-ID 81).

---

## ✅ Resolved Issues (Nov1.1)

### 1. ISSUE-REVOL32.0-F1: Backfill Recovery Contention Audit
- **Symptoms**: Risk of redundant backfill requests and duplicate telemetry ingestion in `MonitorRepository` when multiple peers trigger gap detection simultaneously or when live updates arrive during an active backfill. This leads to network flooding and potential UI jitter.
- **Root Cause**: `MonitorRepository` lacked range-aware deduplication. Any gap detection would trigger a new `BackfillRequest` without checking if that specific range was already being fetched. Additionally, the ingestion pipeline needed to ensure atomicity during multi-peer responses.
- **Resolution**: Hardened `MonitorRepository` with a `rangeMutex` and an `activeRequests` map to track pending backfill ranges. Implemented logic to skip new requests if they overlap with an existing active range. Verified zero redundant requests and data integrity under concurrent simulation in `BackfillContentionTest`. (R-ID 80).

---

## ✅ Resolved Issues (Nov1.0)

### 1. ISSUE-REVOL31.9-F2: Foreground Service Permission Remediation
- **Symptoms**: `java.lang.SecurityException` on API 34+ when starting `SensorService`. The service failed to launch as a foreground service because location permissions were not granted or the service type was not correctly handled during startup.
- **Root Cause**: Missing runtime permission guards in `SensorService.onStartCommand` and `BootReceiver`. On API 34 (Android 14), starting a Foreground Service of type `location` requires active `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`. Additionally, the service type bits in `startForeground` needed alignment with the manifest declarations (including `dataSync` for backfill).
- **Resolution**: Implemented `hasRequiredPermissions()` guards in `SensorService` and `BootReceiver`. The service now self-stops if permissions are missing, preventing the crash. Updated `startForeground` to explicitly include `FOREGROUND_SERVICE_TYPE_LOCATION`, `FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING`, and `FOREGROUND_SERVICE_TYPE_DATA_SYNC` on API 34+. Verified successful service lifecycle on API 34 targets. (R-ID 10).
