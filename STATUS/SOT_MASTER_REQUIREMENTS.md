# SOT Master Requirements - GPS Tracker

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 1. Architectural Authority
*   **1.1 Dependency Injection**: Hilt is the sole authority for dependency management. Manual instantiation of repositories or DAOs is prohibited.
*   **1.2 State Flow**: UI state must be exposed via `StateFlow` from ViewModels. `UiStateAggregator` is the central authority for consolidating telemetry and diagnostic flows (R240). Segmented hydration flows (R248) are required for budget hardware performance.
*   **1.3 Foreground Persistence**: `TrackerService` must maintain a foreground notification. Termination of the service is a violation of SOT.
*   **1.4 Navigation Continuity**: Navigation backstack must be managed to prevent redundant route injection or invalid pop operations. Explicit graph-relative `popUpTo` and `launchSingleTop` are required for all mode transitions (R250).

## 2. Forensic & Performance Requirements
*   **2.1 Sampling Frequency**: Forensic sampling must operate between 10ms and 100ms based on system load (R700).
*   **2.2 Reliability Threshold**: `ALERT_ID_PERFORMANCE_SPIKE` must trigger if `forensicReliability` (EMA) drops below 0.85 for >30s (R715).
*   **2.3 UI Fluidity**: UI stalls (Davey) must not exceed 700ms on target hardware (SM-A155F).
    *   **Status (Aug.21.09)**: 🟢 OPERATIONAL. Segmented hydration and HudUiParts pruning implemented (#248).
*   **2.4 Native Watchdog**: All JNI/native calls must be wrapped in a watchdog timer (2000ms) on `Dispatchers.IO` to prevent hardware hangs from stalling the engine or UI (R301).

## 3. Test & Validation Authority
*   **3.1 Validation Hooks**: The app must provide manual hooks (e.g., `SetForensicSimulation`) to verify alarm triggers under simulated stress (R196-V).
*   **3.2 Auto-Recovery**: System must restore to the previous active mode within 2s of launch (R243).

## 4. History of Changes (Recent)
*   **Aug.22.00**: Hardened Navigation Backstack logic (#250) and implemented JNI Watchdog for native synchronization (#301).
*   **Aug.21.09**: Implemented segmented HUD hydration (#248), background JNI loading (#265), and native resource hardening (#249).
*   **Aug.21.08**: Integrated `UiStateAggregator` (R240) and validation hooks (R196-V).
*   **Aug.21.00**: SOT Alignment for recovery timing (R243).
