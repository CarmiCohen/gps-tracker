# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov1.6)

## 🎯 Next Resumption Focus: Adaptive Power Scaling Logic (Revol37.0)

---

## 🔴 Open Gaps (Nov1.6)
*   *None.*

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL30.0-S1**: Shared Test Fixtures. Consolidate hardware mocks into a test-only artifact within `:core-shared` to reduce duplication across app modules.
*   **SIMP-REVOL33.0-S1**: Batch Buffer Pooling. Consider using an object pool for telemetry batch JSON arrays to reduce GC pressure during long soak periods.
*   **SIMP-REVOL35.0-S1**: Thermal Status Mapping. Consider a more granular mapping of Android Thermal Status levels to specific Celsius offsets to improve throttling precision on diverse hardware.
*   **SIMP-REVOL36.0-S1**: Watchdog Metric Export. Consider exporting watchdog reset counts as a separate telemetry field to track device stability in the field.
*   **SIMP-REVOL37.0-S1**: Hilt Test Module Consolidation. Consider creating a `core-test` module to host common Hilt test overrides (like `FakeSignalingClient`) to avoid duplicate binding issues in multi-module setups.

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.6)
*   **ISSUE-REVOL36.1-F1**: Hilt Test Infrastructure Hardening. **RESOLVED.** Fixed duplicate Dagger bindings in `:app-monitor` instrumentation tests by ensuring `TestInstallIn` replaces all conflicting modules (`MonitorModule` and `NetworkModule`). Standardized `FakeSignalingClient` to satisfy all `SignalingClient` interface requirements. Verified successful execution of `BackfillContentionTest`. (R-ID 84).

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.5)
*   **ISSUE-REVOL36.0-F1**: Forensic Heartbeat & Watchdog. **RESOLVED.** Implemented a dedicated watchdog in `SensorService` that monitors `TickOrchestrator` heartbeats. The loop is automatically restarted if a stall > 3x interval is detected. Verified with `WatchdogRecoveryTest` and hardened `SocketioSignalingClient` to prevent data loss during loop resets. (R-ID 85).
*   **ISSUE-REVOL35.0-F2**: Backfill Audit Infrastructure Fix. **RESOLVED.** Resolved Hilt compilation failure in `:app-monitor` by de-nesting `@TestInstallIn` modules and fixing `FakeSignalingClient` implementation to support batching. Corrected `core-network` dependency scope (`api`) to expose socket types for testing. Verified zero redundant requests in `BackfillContentionTest`. (R-ID 80).

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.4)
*   **ISSUE-REVOL35.0-F1**: Battery-Aware Thermal Throttling. **RESOLVED.** Implemented multi-stage thermal and battery level throttling (10Hz/1Hz/0.1Hz) in `SensingEngine` and `SensorService`. Integrated `BatteryReceiver` and `OnThermalStatusChangedListener` to ensure immediate loop adaptation. Verified constraint priority (Thermal > Battery > Velocity) in `ThermalThrottlingTest` and `ProductionReadinessAuditTest`. (R-ID 83).

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.3)
*   **ISSUE-REVOL34.0-F1**: Adaptive Sampling Logic. **RESOLVED.** Implemented velocity-based telemetry sampling (10Hz moving, 1Hz slow, 0.1Hz stationary) in `SensorService`. Integrated `FusedLocationProviderClient` for real-time speed monitoring and used `TickOrchestrator.preemptLoop` to ensure sub-second adaptation to movement transitions. (R-ID 82).

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.2)
*   **ISSUE-REVOL33.0-F1**: Forensic Signaling Batching. **RESOLVED.** Implemented adaptive telemetry batching (500ms / 10-item limit) in `SocketioSignalingClient`. Integrated **Critical Event Protection** to ensure immediate flush for violations and geofence transitions. Verified throughput optimization and real-time delivery in `SignalingBatchTest` and `PipelineSoakTest`. (R-ID 81).

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.1)
*   **ISSUE-REVOL32.0-F1**: Backfill Recovery Contention Audit. **RESOLVED.** Implemented range-based request deduplication in `MonitorRepository` using a `Mutex`-protected active request map. Verified zero redundant requests during concurrent gap detection in `BackfillContentionTest`. (R-ID 80).

---

## ✅ Resolved Traceability & Metadata Issues (Nov1.0)
*   **ISSUE-REVOL31.9-F2**: Foreground Service Permission Remediation. **RESOLVED.** Implemented API 34 runtime permission guards in `SensorService` and `BootReceiver`. Aligned FGS type bits (`LOCATION|DATA_SYNC|REMOTE_MESSAGING`) with manifest and operational telemetry needs. (R-ID 10).

---

## 📊 Hardening Progress Dashboard
- **Nov1.6: [SOT Count: 891 (Rules: 391), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:5, Testing: PASSED (Infrastructure Fix), QA: 891]**
- **Nov1.5: [SOT Count: 891 (Rules: 391), Open: H:0, M:0, L:0, Ideas: H:0, M:0, L:4, Testing: PASSED (Watchdog & Backfill), QA: 891]**
- **Nov1.4: [SOT Count: 889 (Rules: 389), Open: H:1, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: FAILED (Backfill Contention), QA: 889]**
- **Nov1.3: [SOT Count: 888 (Rules: 388), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: PASSED (Adaptive Sampling), QA: 888]**
- **Nov1.2: [SOT Count: 887 (Rules: 387), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:2, Testing: PASSED (Signaling Batching), QA: 887]**
- **Nov1.1: [SOT Count: 886 (Rules: 386), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: PASSED (Backfill Contention), QA: 886]**
- **Nov1.0: [SOT Count: 885 (Rules: 385), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: PASSED (FGS Permission), QA: 885]**
