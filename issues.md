# Project Issues & Hardening Tracking (Sep.06.30)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.06.30)
*   **Issue #925 RESOLVED: Async Teardown Race Condition**. Remediated critical race condition where rapid `stop() -> start()` sequences attempted re-initialization before the forensic settling window (800ms) completed. Converted `HardwareProvider.start()` to a suspend function that joins the `teardownJob` for deterministic lifecycle transitions (R925/R-ID 273).

## 🟢 Recently Resolved Issues (Sep.06.20)
*   **Issue #924 RESOLVED (Part B): A15 Resource Throttling**. Migrated GNSS throttling logic to `HardwareProvider`. Updates are now capped at 5000ms on A15 hardware during High Load or MaliAnomaly states (R-ID 267).

## 🟢 Recently Resolved Issues (Sep.06.17)
*   **Issue #922 RESOLVED (Part B): HardwareProvider Extraction**. Extracted GNSS jitter monitoring, sensor rate audits (R-ID 256), and energy footprint snapshots (R-ID 259) into `ForensicAuditor`.

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)

### 1. Issue #926: [CRITICAL] Missing Integration: Revival Flow in TrackerService
*   **Description**: `HardwareProvider` emits `revivalEvents` (Attempts, Hardware Lock, and Energy Footprints), but `TrackerService` does not collect this flow.
*   **Risk**: Energy footprint verdicts (R-ID 259) are not transmitted to the viewer. `ALERT_ID_GPS_HARDWARE_LOCK` emitted by `IntegrityMonitor` is ignored in the Service, preventing critical alarms from reaching the HUD.
*   **Action**: Implement a collector for `hardwareProvider.revivalEvents` in `TrackerService`.

### 2. Issue #927: [HIGH] Inconsistency: Safe-Mode vs. GNSS Revival Lifecycle
*   **Description**: `CommunicationManager` suppresses signaling in "Safe Mode" (R-ID 271), but `HardwareProvider` continues aggressive GNSS revival pulses.
*   **Risk**: Excessive battery drain attempting to revive hardware that cannot report its status due to a signaling hang.
*   **Action**: Update `HardwareProvider.checkRevivalLifecycle` to honor the `isSafeMode` state and back off revival frequency.

### 3. Issue #928: [MEDIUM] Unfinished Integration: Performance Alarm Mapping
*   **Description**: `IntegrityMonitor` now sustains violations for `ALERT_ID_SILENT_FAILURE` and `ALERT_ID_PERFORMANCE_SPIKE`, but `TrackerService.observeIntegrityEvents` only handles `ALERT_ID_TRACKER_POWER`.
*   **Action**: Map all critical integrity violations to the `AlarmManager` in `TrackerService`.

### 4. Issue #929: [MEDIUM] Edge Case: Mali Anomaly Exit Hysteresis
*   **Description**: `HardwareProvider` toggles GNSS throttling (5000ms) immediately based on the `maliAnomaly` flag.
*   **Risk**: Rapidly toggling anomaly states could cause "sampling jitter" on budget hardware.
*   **Action**: Implement a 10s cooldown/hysteresis before returning to standard sampling rates after an anomaly clears.

### 5. Issue #930: [LOW] UI Verification: Hist and Details buttons in Event List
*   **Description**: Verify that the "Hist" and "Details" buttons in the event list are working correctly across different alert types.
*   **Action**: Audit navigation logic in `SharedUiComponents.kt` and `ViewerScreen.kt` to ensure correct deep-linking to history and diagnostic details.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 284 (Rules: 52, IDs: 243), Resolved: 925, Open: 5, Testing: 90%, Ideas: 222, QA: 252]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.30)*
