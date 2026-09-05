# Forensic State Snapshot (vSep.05.15) - FINAL HANDOVER

## 🎯 Resumption Focus: Physical Validation (SM-A155F)
The project has completed the **Issue #917** synchronization cycle. All HUD diagnostic indicators (LEDs) are now role-agnostic and reflect the exact actual status of the local engine and peer telemetry pipeline.

### 🟢 Completed: Exact Actual LED Synchronization (vSep.05.15)
Standardized the diagnostic surface to eliminate role-based hardcoding and synchronized all staleness gates to **35 seconds**.

#### 1. Logic Authority: `DashboardStateProviderImpl.kt`
*   **WATCHDOG (WDG)**: Remediated from hardcoded `true` (Tracker) to a strict pulse monitor: `(now - diagnosticState.pulse) < 35000ms`. This ensures the LED reflects the actual health of the background `TrackerService` or `ViewerService`.
*   **DATA (DAT)**: Standardized to monitor the full end-to-end pipeline: `isTelemetryFresh` (Peer) && `isInternet` && `isRelayConnected`.
*   **PEER (VWR/TRK)**: Monitors `isTelemetryFresh` (last remote activity < 35s).
*   **GNSS (GPS)**: Monitors `isGpsFresh` (Total age: Telemetry Age + Source GPS Age < 35s).

#### 2. UI Manifest: `SharedUiComponents.kt`
*   **StatusBar**: Refactored to include `WDG` and `DAT` badges for both roles.
*   **LED Color Parity**: All badges now use `StatusBadge` (Green/Red) driven by the logic above.
*   **Visual Polish**: Converted the Watchdog "OK/FAIL" text to a standard "WDG" badge for architectural consistency.

#### 3. Threshold Synchronization: `EngineConstants.kt`
*   Locked `TELEMETRY_UI_STALE_THRESHOLD_MS` and `GPS_UI_FAIL_THRESHOLD_MS` to **35,000ms** (R338/R972).

### 🟡 Open Issues & Verification
*   **Issue #916**: Battery Drain Audit. Monitor the impact of the **Raw Location Provider Bypass** on Samsung A15.
*   **Soak Test**: Verify long-term stability of the 35s heartbeat logic during extended deep-sleep cycles.

## 🛠️ Architectural State
- **Target SDK:** 35 (Android 15)
- **Hardening:** `HIGH_SAMPLING_RATE_SENSORS` permission declared and verified.
- **Hydration:** 11-tier level established. Nav guards in `MainAppContent` prevent accidental session termination during hydration gaps.

## 💾 Release Archive (Git)
*   **Commit:** `6704e37` (Hardening: Resolved Issue #917)
*   **Tag:** `vSep.05.15` (Synchronization of HUD Staleness Gates)

## 📊 Current Audit Baseline
- **Current Audit Baseline: [SOT: 265 (Rules: 43, IDs: 222), Resolved: 891, Open: 1 (#916), Testing: 1 (Sub-items: 12), Ideas: 7, QA: 242]**
