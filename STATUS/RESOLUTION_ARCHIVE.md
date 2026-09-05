# Resolution Archive (Sep.05.24)

## 🟢 Resolved Issues (Sep.05.24)
*   **Issue #910 RESOLVED: Hydration Watchdog**. Implemented a 15s hydration watchdog (`WATCH_DOG_UI_GRACE_MS`) in `MainViewModel`. The system now monitors startup hydration levels and forces the UI initialization state (`isInitialized = true`) if a hang is detected at Level 2, ensuring a recovery path from black-screen locks on budget hardware (R-ID 261).

## 🟢 Resolved Issues (Sep.05.23)
*   **Issue #916 RESOLVED: GNSS Revival Lifecycle Transparency & Battery Audit**. Finalized the GNSS recovery event chain in `HardwareProvider.kt` and instrumented `IntegrityMonitor.kt` to log battery current (mA) snapshots during revival pulses. Remediated "Silent Failure" risks on budget hardware (A15) and enabled empirical quantification of the energy footprint of the recovery mechanism (R-ID 259/260).
*   **Issue #918 RESOLVED: Clock Source Consistency**. RESOLVED a logic defect where peer activity was tracked using monotonic `elapsedRealtime()` but evaluated against wall-clock `currentTimeMillis()`. Standardized all staleness gates to strictly use `SystemClock.elapsedRealtime()` to ensure the `VWR` badge accurately reflects peer presence without being affected by NTP regressions or user clock changes (R919/R-ID 257).
*   **Issue #918 RESOLVED: VWR Badge Persistence Leak**. RESOLVED by restricting `lastRemoteActivityTs` updates to high-assurance telemetry (Location/Health) packets. Removed generic signaling heartbeats (pulses/pongs) from the freshness calculation to ensure the `VWR` badge accurately reflects the Peer app's lifecycle state. Enabled binary telemetry processing on the Tracker to support high-efficiency Viewer pulses (R-ID 258).
*   **Issue #917 RESOLVED: Exact Actual Colors (HUD LED Synchronization)**. RESOLVED by remediating hardcoded "always-true" telemetry freshness logic in `DashboardStateProviderImpl`. Standardized all HUD LEDs (**WDG**, **DAT**, **VWR/TRK**) to monitor actual service pulses and peer activity with a synchronized **35s** staleness gate. Verified role parity where both Tracker and Viewer reflect internal service integrity (R-ID 257).
*   **Issue #915 RESOLVED: Mapnik Tile Latency (Samsung A15)**. Remediated map interaction lag on budget hardware by implementing specialized resource profiles (R915). Expand disk cache to 600MB and throttled concurrency (R915).

## 🟢 Resolved Issues (Sep.05.22)
*   **Issue #910 RESOLVED: Forensic Stall Simulation (Service Termination Race)**. Remediated a race condition during UI hydration where a transient `null` `appMode` triggered a navigation retreat to the `Landing` route. Hardened `MainAppContent.kt` with a guard preventing Landing navigation if `isSystemActive` is true (R-ID 255).
