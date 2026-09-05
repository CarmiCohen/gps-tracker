# Project Issues & Hardening Tracking (Sep.05.10)

## 🎯 Current Resumption Focus: Physical Verification (A15)
Critical recovery phase for system-wide connectivity and GNSS stability. 
**Next Step:** Verify fix for Issue #910 and monitor A15 power consumption (Issue #916).

## 🟢 Recently Resolved Issues (Sep.05.10)
*   **Issue #910: Forensic Stall Simulation (Service Termination Race)**. RESOLVED by hardening MainAppContent navigation logic. Added a guard to prevent Landing navigation (and exit triggers) when isSystemActive is true, even if appMode is transiently null during hydration (R910/R255).
*   **Issue #912: Signaling Relay Connection Failure (CRITICAL)**. Resolved protocol mismatch by finalizing Socket.io at v2.1.2 and forcing strict WebSocket transport to bypass Render XHR errors.
*   **Issue #905: GNSS "Zombie" State (Budget Hardware)**. Resolved by implementing a Raw Provider Bypass in HardwareProvider.kt, forcing chipset rescans via direct GPS_PROVIDER requests.
*   **Issue #893: Hardware Looper Contention**. Resolved by moving all location callbacks to a dedicated hardware thread, protecting the UI from high-frequency jitter.
*   **Issue #914: UI Flow Optimization**. Removed redundant distinctUntilChanged from StateFlows in MainViewModel to satisfy Kotlin 2.0 strictness.

## 🟡 Open Issues (Verification Phase)
*   **Issue #915: Mapnik Tile Latency**. Investigating staggered tile loading for A15.
*   **Issue #916: Battery Drain Audit**. Monitoring drain during active raw GNSS bypass.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 911 (Rules: 42, IDs: 198), Resolved: 888, Open: 2 (#915, #916), Testing: 1 (Sub-items: 12), Ideas: 5, QA: 244]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.05.10)*
