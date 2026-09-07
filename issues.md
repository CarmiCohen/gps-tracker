# Project Issues & Hardening Tracking (Sep.06.50)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.06.50)
*   **Issue #932 RESOLVED: HUD Synchronization**. Synchronized the HUD status bar to reflect Samsung A15 hardware adaptations. Propagated `isA15Device` from the hardware layer through `MainViewModel` and `UiStateAggregator`. Added a new `A15` badge to the `StatusBar` in `SharedUiComponents.kt` to provide forensic confirmation that background "Poke" logic and `specialUse` FGS adaptations are active (R-ID 276).

## 🟢 Recently Resolved Issues (Sep.06.45)
*   **Issue #931 RESOLVED: GPS Reception Parity (Viewer Mode)**. Remediated discrepancy where the Viewer role suffered from background GPS suppression on Samsung A15 hardware compared to Waze/Maps. Promoted `ViewerService` to include `specialUse` FGS type, implemented A15 "Poke" logic (30s hardware handshake/WakeLock), and added adaptive polling (2000ms when UI is active) to keep the GPS pipeline fresh (R-ID 276).

## 🟢 Recently Resolved Issues (Sep.06.35)
*   **Issue #930 RESOLVED: Event List Deep-Linking**. Added functional "HIST" and "DIAG" buttons to the `LogDetailPane` in `LogOverlay`. Users can now navigate from a specific forensic log entry directly to the analytical history ribbons (synchronized with the log's timestamp via replay cursor) or the system diagnostics screen (R-ID 930/275).

## 🟢 Recently Resolved Issues (Sep.06.33)
*   **Issue #929 RESOLVED: Mali Anomaly Exit Hysteresis**. Implemented a 10s cooldown period in `HardwareProvider` before returning to standard sampling rates after an anomaly (High Load or Mali Anomaly) clears. This prevents "sampling jitter" on budget hardware like the Samsung A15 (R-ID 274).

## 🟢 Recently Resolved Issues (Sep.06.32)
*   **Issue #927 RESOLVED: Safe-Mode vs. GNSS Revival**. Updated `HardwareProvider` to honor the `isSafeMode` state, preventing battery-draining revival pulses during signaling recovery hangs (R-ID 271). Fixed logic inversion in permission auditing.

## 🟢 Recently Resolved Issues (Sep.06.31)
*   **Issue #926 RESOLVED: Revival Integration**. Implemented collector for `hardwareProvider.revivalEvents` in `TrackerService`. Energy footprint verdicts (R-ID 259) are now transmitted as important logs to the viewer. Hardware locks are now propagated through the entire logic chain to the HUD.
*   **Issue #928 RESOLVED: Integrity Mapping**. Mapped all critical integrity violations (Silent Failure, Performance Spikes, Hardware Lock) from `IntegrityMonitor` to `AlarmManager` to ensure they trigger valid system alerts (R-ID 272).

## 🟢 Recently Resolved Issues (Sep.06.30)
*   **Issue #925 RESOLVED: Async Teardown Race Condition**. Remediated critical race condition where rapid `stop() -> start()` sequences attempted re-initialization before the forensic settling window (800ms) completed (R925/R-ID 273).

## 🟡 Open Issues & Hardening Tasks (Sorted by Recommended Priority)
*(No open high-priority issues)*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 288 (Rules: 50, IDs: 238), Resolved: 932, Open: 0, Testing: 90% (Sub-items: 45), Ideas: 223, QA: 252]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.50)*
