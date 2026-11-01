# Project Issues & Hardening Tracking - REVOLUTION PHASE (Nov1.4)

## 🎯 Next Resumption Focus: Forensic Heartbeat & Watchdog (Revol36.0)

---

## 🔴 Open Gaps (Nov1.4)
*   *None. Battery-aware thermal throttling gaps resolved.*

---

## 🚀 Strategic Simplification Ideas
*   **SIMP-REVOL30.0-S1**: Shared Test Fixtures. Consolidate hardware mocks into a test-only artifact within `:core-shared` to reduce duplication across app modules.
*   **SIMP-REVOL33.0-S1**: Batch Buffer Pooling. Consider using an object pool for telemetry batch JSON arrays to reduce GC pressure during long soak periods.
*   **SIMP-REVOL34.0-S1**: Sampling State Machine. **IMPLEMENTED.** Sensing loop now dynamically adapts to velocity (10Hz/1Hz/0.1Hz) using FusedLocation speed.
*   **SIMP-REVOL35.0-S1**: Thermal Status Mapping. Consider a more granular mapping of Android Thermal Status levels to specific Celsius offsets to improve throttling precision on diverse hardware.

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
- **Nov1.4: [SOT Count: 889 (Rules: 389), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: PASSED (Thermal Throttling), QA: 889]**
- **Nov1.3: [SOT Count: 888 (Rules: 388), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: PASSED (Adaptive Sampling), QA: 888]**
- **Nov1.2: [SOT Count: 887 (Rules: 387), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:2, Testing: PASSED (Signaling Batching), QA: 887]**
- **Nov1.1: [SOT Count: 886 (Rules: 386), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: PASSED (Backfill Contention), QA: 886]**
- **Nov1.0: [SOT Count: 885 (Rules: 385), Open: H:0, M:0, L:0, Ideas: H:1, M:0, L:1, Testing: PASSED (FGS Permission), QA: 885]**
