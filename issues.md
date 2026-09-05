# Project Issues & Hardening Tracking (Sep.05.23)

## 🎯 Current Resumption Focus: Physical Verification (A15)
Critical recovery phase for system-wide connectivity and GNSS stability. 

## 🟢 Recently Resolved Issues (Sep.05.23)
*   **Issue #916 RESOLVED: GNSS Revival Lifecycle Transparency & Battery Audit**. Finalized the GNSS recovery event chain in `HardwareProvider.kt` and instrumented `IntegrityMonitor.kt` to log battery current (mA) snapshots during revival pulses. Remediated "Silent Failure" risks on budget hardware (A15) and enabled empirical quantification of the energy footprint of the recovery mechanism (R-ID 259/260).
*   **Issue #918 RESOLVED: Clock Source Consistency**. RESOLVED a logic defect where peer activity was tracked using monotonic `elapsedRealtime()` but evaluated against wall-clock `currentTimeMillis()`. Standardized all staleness gates to strictly use `SystemClock.elapsedRealtime()` to ensure the `VWR` badge accurately reflects peer presence without being affected by NTP regressions or user clock changes (R919/R-ID 257).
*   **Issue #918 RESOLVED: VWR Badge Persistence Leak**. RESOLVED by restricting `lastRemoteActivityTs` updates to high-assurance telemetry (Location/Health) packets. Removed generic signaling heartbeats (pulses/pongs) from the freshness calculation to ensure the `VWR` badge accurately reflects the Peer app's lifecycle state. Enabled binary telemetry processing on the Tracker to support high-efficiency Viewer pulses (R-ID 258).
*   **Issue #917 RESOLVED: Exact Actual Colors (HUD LED Synchronization)**. RESOLVED by remediating hardcoded "always-true" telemetry freshness logic in `DashboardStateProviderImpl`. Standardized all HUD LEDs (**WDG**, **DAT**, **VWR/TRK**) to monitor actual service pulses and peer activity with a synchronized **35s** staleness gate. Verified role parity where both Tracker and Viewer reflect internal service integrity (R-ID 257).
*   **Issue #915 RESOLVED: Mapnik Tile Latency (Samsung A15)**. Remediated map interaction lag on budget hardware by implementing specialized resource profiles (R915).

## 🟡 Open Issues (Verification Phase)
*   *None. System baseline reached for Sep.05.23.*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 270 (Rules: 45, IDs: 225), Resolved: 896, Open: 0, Testing: 1 (Sub-items: 12), Ideas: 9, QA: 242]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.05.23)*
