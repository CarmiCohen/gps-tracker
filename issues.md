# Project Issues & Hardening Tracking (Sep.10.20)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority open issues.*

## 🟢 Recently Resolved Issues (Sep.10.20)
*   **Map State Partitioning RIGOROUS AUDIT (#243-Audit)**: Performed a deep-forensic audit of the Map UI layer to eliminate remaining derived state and redundant parameters.
    *   **Root-Cause Remediation**: Consolidated `MapToolsOverlay` and marker freshness logic into `MapViewState`. Moved staleness calculations (15s gate) into the `MainViewModel` flow to ensure UI-side calculations are entirely eliminated. Simplified `AppMapContainer` signature to consume a single state object, fulfilling the strict interpretation of R-ID 287.

## 🟢 Recently Resolved Issues (Sep.11.10)
*   **Map Partitioning Integrity RESOLVED (#243-Audit-Initial)**: Performed an initial integrity audit of the Map State Partitioning implementation.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 311 (Rules: 58, IDs: 253), Resolved: 979, Open: 0, Testing: 100% (Sub-items: 50), Ideas: 3, QA: 269]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.10.20)*
