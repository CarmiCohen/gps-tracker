# Project Issues & Hardening Tracking (Sep.06.32)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.06.32)
*   **Issue #927 RESOLVED: Safe-Mode vs. GNSS Revival**. Updated `HardwareProvider` to honor the `isSafeMode` state, preventing battery-draining revival pulses during signaling recovery hangs (R-ID 271). Fixed logic inversion in permission auditing.

## 🟢 Recently Resolved Issues (Sep.06.31)
*   **Issue #926 RESOLVED: Revival Integration**. Implemented collector for `hardwareProvider.revivalEvents` in `TrackerService`. Energy footprint verdicts (R-ID 259) are now transmitted as important logs to the viewer. Hardware locks are now propagated through the entire logic chain to the HUD.
*   **Issue #928 RESOLVED: Integrity Mapping**. Mapped all critical integrity violations (Silent Failure, Performance Spikes, Hardware Lock) from `IntegrityMonitor` to `AlarmManager` to ensure they trigger valid system alerts (R-ID 272).

## 🟢 Recently Resolved Issues (Sep.06.30)
*   **Issue #925 RESOLVED: Async Teardown Race Condition**. Remediated critical race condition where rapid `stop() -> start()` sequences attempted re-initialization before the forensic settling window (800ms) completed (R925/R-ID 273).

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)

### 1. Issue #929: [MEDIUM] Edge Case: Mali Anomaly Exit Hysteresis
*   **Description**: `HardwareProvider` toggles GNSS throttling (5000ms) immediately based on the `maliAnomaly` flag.
*   **Risk**: Rapidly toggling anomaly states could cause "sampling jitter" on budget hardware.
*   **Action**: Implement a 10s cooldown/hysteresis before returning to standard sampling rates after an anomaly clears.

### 2. Issue #930: [LOW] UI Verification: Hist and Details buttons in Event List
*   **Description**: Verify that the "Hist" and "Details" buttons in the event list are working correctly across different alert types.
*   **Action**: Audit navigation logic in `SharedUiComponents.kt` and `ViewerScreen.kt` to ensure correct deep-linking to history and diagnostic details.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 285 (Rules: 50, IDs: 235), Resolved: 928, Open: 2, Testing: 90%, Ideas: 222, QA: 252]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.32)*
