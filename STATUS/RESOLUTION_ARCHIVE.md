# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 713**

## 107. Navigation State Persistence & Mode Recovery (Aug.25.01)
*   **Issue #311: Mode Transition Navigation Regression**.
    - **Resolution**: Migrated navigation selection state (`isManualSelectionInProgress`, `isSettlingActive`) from local `remember`ed variables to the centralized `MainUiState`.
    - **Action**: Implemented `SetManualSelection` and `SetSettlingActive` events in `MainViewModel`. Updated `MainAppContent.kt` to utilize these persistent fields. This prevents the app from reverting to the Landing screen when returning from system permission dialogs (e.g., Background Location request) on devices with aggressive Activity destruction.
    - **Verification**: UI audit confirms navigation continuity across Activity recreation.

## 106. Imperative Map Isolation & Ghost Load Neutralization (Aug.25.00)
*   **Issue #309: Compose Lock Verification Persistent Warnings**.
    - **Resolution**: Replaced `SnapshotStateList` and `SnapshotStateMap` in `MapOverlayManager.kt` with standard `ArrayList` and `HashMap` collections. 
    - **Action**: High-frequency map updates are performed imperatively within `AndroidView.update`. Using standard collections eliminates lock contention and frame skips on A15 hardware.
*   **Issue #310: libmbrainSDK Ghost Load Persistence**.
    - **Resolution**: Neutralized legacy signatures in `JdHardwareManager.kt` to prevent Samsung CFMS heuristic triggers.

## 105. Monotonic Authority & Maintenance Uptime Hardening (Aug.24.01)
*   **Issue #307: Inconsistent Maintenance Uptime Logging**.
    - **Resolution**: Standardized health-check durations using monotonic time (`elapsedRealtime`).

*(Older resolutions preserved in Git history)*
