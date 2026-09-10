# Project Issues & Hardening Tracking (Sep.10.30)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority open issues identified.*

## 🟢 Recently Resolved Issues (Sep.10.30)
*   **S21 FE Reactive Flow Stall & Viewer Recovery Loop RESOLVED (#945)**:
    *   **Root-Cause Remediation**: Hardened `SystemMonitor` grid-aligned scheduling to prevent "danger window" overlaps that triggered reactive flow stalls on Samsung S21 FE hardware. By ensuring alarms are never scheduled within 20s of the current time, the recovery loop is eliminated (R-ID 302).
*   **Peer Status LED Verification (#943)**: Verified role-appropriate LED logic (VWR/TRK) on S21 and A15 hardware. Confirmed isolated devices correctly default to Red, and established handshakes transition to Cyan/Green.
*   **Map State Partitioning RIGOROUS AUDIT (#243-Audit)**: Performed a deep-forensic audit of the Map UI layer to eliminate remaining derived state and redundant parameters.
    *   **Root-Cause Remediation**: Consolidated `MapToolsOverlay` and marker freshness logic into `MapViewState`. Moved staleness calculations (15s gate) into the `MainViewModel` flow to ensure UI-side calculations are entirely eliminated. Simplified `AppMapContainer` signature to consume a single state object, fulfilling the strict interpretation of R-ID 287.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 311 (Rules: 58, IDs: 253), Resolved: 981, Open: 0, Testing: 100% (Sub-items: 50), Ideas: 3, QA: 269]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.10.30)*
