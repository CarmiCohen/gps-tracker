# Project Issues & Hardening Tracking (Sep.11.10)

## 🎯 Current Resumption Focus: Background Service Stability & Grid Precision
Watchdog precision gaps and GNSS hysteresis for Android 15 have been fully remediated. Focus shifts to long-term lifecycle stability and potential simplification of the service orchestration layer.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *No high-priority open issues.*

## 🟢 Recently Resolved Issues (Sep.11.10)
*   **Map Partitioning Integrity RESOLVED (#243-Audit)**: Performed a rigorous integrity audit of the Map State Partitioning implementation.
    *   **Root-Cause Remediation**: Eliminated "leftovers of leftovers" by removing redundant map tool overlays and individual parameters from `TrackerScreen.kt` and `ViewerScreen.kt`. Optimized `MainViewModel.kt` with trigger pruning via `MapUiParts` to ensure map state updates only on relevant changes (R-ID 287).

## 🟢 Recently Resolved Issues (Sep.10.12)
*   **Map State Partitioning RESOLVED (#243)**: Refactored the Map UI layer to use a consolidated `MapViewState` object.
    *   **Root-Cause Remediation**: Bundled ~40 individual map parameters into a structured `MapViewState` in `MainUiState.kt`. Updated `MainViewModel.kt` to aggregate these into a single flow and refactored `AppMapContainer` and `OsmMap` to consume the state (R-ID 287).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 311 (Rules: 58, IDs: 253), Resolved: 978, Open: 0, Testing: 100% (Sub-items: 50), Ideas: 3, QA: 269]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.10)*
