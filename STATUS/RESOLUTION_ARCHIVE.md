# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 675**

## 92. Anchor & HUD Hardening (Aug.20.10)
*   **Issue #238: Anchor Evaluator Coordinate Leak**.
    - **Resolution**: Hardened stationary anchoring by restricting coordinate-averaging convergence to points within the 50% "dead zone" of the current breakout threshold. This prevents anchors from "chasing" GPS drift and ensures reliable breakouts in high-SNR scenarios. (R238).
*   **Issue #239: Undefined addPersistentLog**.
    - **Resolution**: Restored the missing `addPersistentLog` implementation in `MainViewModel.kt` and `MainFileHelper.kt` to resolve critical compilation failures across 20+ call sites. (R239).
*   **Issue #240: HUD Component API Drift**.
    - **Resolution**: Migrated `TrackerScreen.kt`, `ViewerScreen.kt`, and `GlobalStatusBar` to consume the unified `HudState` architecture, eliminating legacy parameter mismatches. (R240).
*   **Issue #241: ViewModel Combine Mismatch**.
    - **Resolution**: Synchronized `MainViewModel` combine blocks with updated `DashboardStateProvider` signatures to resolve type-inference and parameter-count errors. (R241).
*   **Issue #235: Missing Dependency Hardening**.
    - **Resolution**: Restored explicit dependency declarations for `DataStore`, `Protobuf`, and `Socket.io` in `app/build.gradle` that were causing package-level unresolved references. (R235).

## 91. Diagnostics Visibility Authority (Aug.20.09)
*   **Issue #233: Diagnostics Screen Visibility layering**.
    - **Resolution**: Modified `MainAppContent.kt` to ensure the `DiagnosticsScreen` is never obscured by initial setup overlays. Tapping "VIEW SYSTEM DIAGNOSTICS" now triggers an explicit dismissal of the `PhoneSetupOverlay`, guaranteeing that technical telemetry and diagnostic data are immediately visible to the user. (R233).

## 90. UI Hardening: Action Button Clipping (Aug.20.08)
*   **Issue #232: PhoneSetup Action Button Clipping**.
    - **Resolution**: Refined the button layout in `PhoneSetupOverlay` and `CleanSetupOverlay` to prevent text truncation on low-density hardware (SM-A155F). Converted fixed `height()` modifiers to flexible `heightIn(min = ...)` to allow for vertical expansion. Optimized action button font sizes to `13.sp` to ensure text wrapping remains legible and unclipped under various display scales. (R232).

## 89. Analytical Telemetry Optimization (Aug.20.07)
*   **Issue #225: Analytical Telemetry Optimization**.
    - **Resolution**: Consolidated the 1:1 parity transformation for forensic metadata into a unified `ForensicMapper.kt`. Refactored `MainRepository.kt`, `HistoryManager.kt`, and `ConnectivitySuite.kt` to utilize this master translator. Added `isBatteryLow` and `isBatteryCritical` to `EngineConnectionPoint` to ensure full architectural parity across engine, persistence, and UI models. (R225).

## 88. Field Monitoring & Forensic Parity (Aug.20.06)
*   **Issue #224: Field Monitoring & Maintenance**.
    - **Resolution**: Validated coordinate stabilization (R224) with 100m threshold. Implemented full forensic parity for high-frequency vertical velocity metadata across the telemetry pipeline. Synchronized `MainRepository.kt` mapping for `sitVzTs`, `sitVzRt`, and `verticalVelocity`. Bumped database to **v73** with `MIGRATION_72_73`. (R224).

---
*For older resolutions, see history. (vAug.20.10)
