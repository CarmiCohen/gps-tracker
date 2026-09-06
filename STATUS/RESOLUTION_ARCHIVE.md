# Resolution Archive (Sep.06.33)

## 🟢 Resolved Issues (Sep.06.33)
*   **Issue #929 RESOLVED: Mali Anomaly Exit Hysteresis**. Implemented a 10s cooldown period in `HardwareProvider` before returning to standard sampling rates after an anomaly (High Load or Mali Anomaly) clears. This prevents "sampling jitter" on budget hardware like the Samsung A15 (R-ID 274).

## 🟢 Resolved Issues (Sep.06.32)
*   **Issue #927 RESOLVED: Safe-Mode vs. GNSS Revival**. Updated `HardwareProvider` to honor the `isSafeMode` state, preventing battery-draining revival pulses during signaling recovery hangs (R-ID 271). Fixed logic inversion in permission auditing.

## 🟢 Resolved Issues (Sep.06.31)
*   **Issue #926 RESOLVED: Revival Integration**. Implemented collector for `hardwareProvider.revivalEvents` in `TrackerService`. Energy footprint verdicts (R-ID 259) are now transmitted as important logs to the viewer. Hardware locks are now propagated through the entire logic chain to the HUD.
*   **Issue #928 RESOLVED: Integrity Mapping**. Mapped all critical integrity violations (Silent Failure, Performance Spikes, Hardware Lock) from `IntegrityMonitor` to `AlarmManager` to ensure they trigger valid system alerts (R-ID 272).

## 🟢 Resolved Issues (Sep.06.30)
*   **Issue #925 RESOLVED: Async Teardown Race Condition**. Remediated critical race condition where rapid `stop() -> start()` sequences attempted re-initialization before the forensic settling window (800ms) completed (R925/R-ID 273).

## 🟢 Resolved Issues (Sep.06.20)
*   **Issue #924 RESOLVED (Part B): A15 Resource Throttling**. Migrated GNSS throttling from the UI layer (`MainViewModel`) down to the `HardwareProvider` source. Implemented resource-aware emission logic that automatically throttles satellite status updates to 5000ms on A15 hardware when high system load or Mali driver anomalies are detected (R-ID 267).

*(For older resolutions, see history logs.)*
