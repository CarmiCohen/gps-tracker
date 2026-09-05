# Project Issues & Hardening Tracking (Sep.05.15)

## 🎯 Current Resumption Focus: Physical Verification (A15)
Critical recovery phase for system-wide connectivity and GNSS stability. 
**Next Step:** Physical soak test on A15 to verify long-term stability with HIGH_SAMPLING_RATE_SENSORS enabled.

## 🟢 Recently Resolved Issues (Sep.05.15)
*   **Issue #917: Exact Actual Colors (HUD LED Synchronization)**. RESOLVED by remediating hardcoded "always-true" telemetry freshness logic in `DashboardStateProviderImpl`. Standardized all HUD LEDs (**WDG**, **DAT**, **VWR/TRK**) to monitor actual service pulses and peer activity with a synchronized **35s** staleness gate. Verified role parity where both Tracker and Viewer reflect internal service integrity (R-ID 257).
*   **Issue #915: Mapnik Tile Latency (Samsung A15)**. RESOLVED by implementing R915 budget-hardware profile. Throttled download concurrency to 2 threads to reduce Helio G99 context switching and expanded disk cache to 600MB/Memory cache to 64 tiles (R915).
*   **Issue #910: Tracker Service "Short-Circuit" (A15/Target SDK 35)**. RESOLVED by declaring `HIGH_SAMPLING_RATE_SENSORS` permission in manifest to remediate `SecurityException` during sensor registration. Further hardened UI via `MainAppContent` navigation guards to prevent accidental `onCleanupAndExit()` triggers during hydration gaps (R910/R255/R256).
*   **Issue #912: Signaling Relay Connection Failure (CRITICAL)**. Resolved protocol mismatch by finalizing Socket.io at v2.1.2 and forcing strict WebSocket transport.
*   **Issue #905: GNSS "Zombie" State (Budget Hardware)**. Resolved by implementing a Raw Provider Bypass in HardwareProvider.kt.
*   **Issue #914: UI Flow Optimization**. Removed redundant distinctUntilChanged from StateFlows in MainViewModel.

## 🟡 Open Issues (Verification Phase)
*   **Issue #916: Battery Drain Audit**. Monitoring drain during active raw GNSS bypass.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 265 (Rules: 43, IDs: 222), Resolved: 891, Open: 1 (#916), Testing: 1 (Sub-items: 12), Ideas: 7, QA: 242]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.05.15)*
