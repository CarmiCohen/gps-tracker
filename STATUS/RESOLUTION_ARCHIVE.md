# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.29.13 (vAug.29.13)
*   **Concern #766 Resolved**: **RTL Layout Inconsistency and Text Truncation**. Identified UI layout flipping on devices with RTL locales, causing technical data to be misaligned. Remediated by enforcing LTR directionality in `StatusBar` via `CompositionLocalProvider`. Additionally, fixed truncation of `LocationPendingReason` (e.g., "SIGNAL LOSS") by adjusting width allocation in `StatusRowData` and setting overflow to `Visible`. (R766).
*   **Completion Sequence**: Finalized UI consistency audit, synchronized status documentation, and incremented app version to `Aug.29.13`.

## 🟢 Aug.29.12 (vAug.29.12)
*   **Concern #762 Resolved**: **Acoustic Refinement (R762b)**. Encapsulated the adaptive acoustic duty-cycle calculation into a standalone pure function (`computeAdaptiveAcousticOffCycle`) in `SentinelValidator.kt`. Refactored `HardwareProvider.kt` to utilize this function, reducing complexity in the acoustic monitoring loop and improving testability by separating calculation logic from hardware side-effects (R762b).
*   **Completion Sequence**: Finalized acoustic logic audit, synchronized all status tracking documentation, and incremented app version to `Aug.29.12`.

## 🟢 Aug.29.11 (vAug.29.11)
*   **Concern #765 Resolved**: **Ultra-Long Stationary State UI Refinement**. Identified a lack of visual feedback in the UI when the system enters the ultra-long GNSS relaxation mode. Remediated by adding `[ULTRA]` badges to the HUD (StatusBar) and the Telemetry Dashboard. This provides full transparency to the user (locally) and viewer (remotely) regarding the system's current power-saving state (R765).
*   **Completion Sequence**: Finalized UI transparency audit, synchronized all status tracking documentation, and incremented app version to `Aug.29.11`.

## 🟢 Aug.29.10 (vAug.29.10)
*   **Concern #765 Resolved**: **Ultra-Long Stationary State Exposure**. Identified a lack of transparency when the system enters ultra-long GNSS relaxation mode. Remediated by centralizing "Ultra-Long Stationary" state logic in `HardwareProvider.kt` and exposing it via `isUltraLongStationaryFlow`. Propagated this state through the `TrackerService` to the `NotificationManager` (for local foreground pulse) and the telemetry aggregation pipeline (for remote viewer transparency). This ensures deterministic awareness of power-saving behaviors (R765).
*   **Completion Sequence**: Finalized hardware transparency audit, incremented database version to 74 with MIGRATION_73_74, and synchronized all status tracking documentation.

## 🟢 Aug.29.09 (vAug.29.09)
*   **Concern #764 Resolved**: **Shared Engine Configuration Refinement**. Identified redundant data structures and mapping overhead in `ServiceBehaviorUseCase.kt`. Remediated by removing the `DeviceSpecialFlags` class and refactoring the component to utilize the engine-level `HardwareCapabilities` model directly. This improves architectural consistency between the core engine and app services (R764).

## 🟢 Aug.29.08 (vAug.29.08)
*   **Concern #763 Resolved**: **Ultra-Long Stationary GNSS Relaxation**. Identified an opportunity for further battery optimization during long-term surveillance. Implemented logic in `ServiceBehaviorUseCase.kt` to relax GNSS polling to 5-minute intervals (`ULTRA_LONG_STATIONARY_GPS_POLLING_MS`) once a device has been confirmed stationary for more than 4 hours (`ULTRA_LONG_STATIONARY_DURATION_MS`). This significantly extends standby time without sacrificing security, as any physical movement immediately resets the interval to high-frequency polling (R763).
*   **Completion Sequence**: Finalized GNSS relaxation audit, updated versioning to `Aug.29.08`, and synchronized all status tracking documentation.

## 🟢 Aug.29.07 (vAug.29.07)
*   **Completion Sequence**: Finalized Acoustic Duty-Cycle Optimization audit, updated versioning to `Aug.29.07`, and synchronized all status tracking documentation.

## 🟢 Aug.29.06 (vAug.29.06)
*   **Concern #762 Resolved**: **Acoustic Duty-Cycle Optimization**. Identified excessive battery drain and native resource churn during long stationary periods due to fixed microphone duty-cycling. Remediated by implementing adaptive off-cycle scaling in `HardwareProvider.kt`. The off-cycle duration now scales dynamically from 8 seconds up to 30 seconds based on the stationary duration (leveraging `stationaryStartRt`), significantly reducing power consumption during idle periods while maintaining security responsiveness (R762).

## 🟢 Aug.29.05 (vAug.29.05)
*   **Concern #761 Resolved**: **Telemetry Mapping Authority**. Identified violation of SRP in `HistoryManager` and logic duplication across services. Remediated by creating `TelemetryMapper.kt`, a centralized authority for coordinate and forensic property parity. (R761).
*   **Legacy Purge**: Permanently decommissioned `GpsManager.kt`, `AppSensorManager.kt`, and `ForensicMapper.kt` following the successful consolidation into `HardwareProvider` and `TelemetryMapper`.

## 🟢 Aug.29.04 (vAug.29.04)
*   **Completion Sequence**: Finalized hardware consolidation audit, updated versioning to `Aug.29.04`, and synchronized all status tracking documentation.

## 🟢 Aug.29.03 (vAug.29.03)
*   **Concern #760 Resolved**: **Hardware Consolidation (Unified Provider)**. Identified architectural fragmentation and redundant thread overhead caused by independent `GpsManager` and `AppSensorManager` instances. Remediated by merging both into a single `HardwareProvider`. This consolidation shares a single optimized "HardwareThread" for all platform callbacks (GNSS, Location, Sensors, Display), reducing context-switching overhead and streamlining the service-level shutdown sequence (R760).

## 🟢 Aug.29.02 (vAug.29.02)
*   **Concern #759b Resolved**: **Trail Polyline Decomposition (Segmented Hydration)**. Identified Main-thread "Davey" stalls (>700ms) on budget hardware when rendering large telemetry trails. Remediated by implementing segmented trail updates in `MapOverlayManager` using coroutines and `yield()`. (R759b).

## 🟢 Aug.29.00 (vAug.29.00)
*   **Concern #758b Resolved**: **Residual UI Thread Congestion (Async Geometry)**. Soak testing on SM-A155F revealed "Davey" warnings (>1000ms) during Map Hydration (Levels 4-7) despite engine pre-warming. Identified the bottleneck as synchronous point-circle generation in `MapOverlayManager`. Remediated by offloading all circle geometry calculations to `Dispatchers.Default` and implementing an async state-matching pattern to ensure the UI thread remains responsive during heavy overlay updates (R758b).

## 🟢 Aug.28.11 (vAug.28.11)
*   **Concern #757 Resolved**: **Persistent BaseEventQueue Leak (Hardening)**. Audit of deployment logs on SM-A155F confirmed residual native resource leaks during service teardown. Identified a lifecycle mismatch where `GpsManager` would skip unregistration if its internal `isStarted` flag was out of sync with active background revival pulses. Hardened `GpsManager` and `AppSensorManager` to perform unconditional unregistration of all GNSS, location, sensor, and display listeners in `stop()`, ensuring deterministic native disposal (R757).
*   **Concern #759 Resolved**: **Logcat Spam Remediation (Hardening)**. Migration of high-frequency permission and capability checks in `SystemStatusProvider` to the `GpsApplication.PACKAGE_NAME` shadow-cache. This eliminated the repetitive `getPackageName` system logs observed during soak testing on the Samsung A15 (R759).

## 🟢 Aug.28.10 (vAug.28.10)
*   **Concern #758 Resolved**: **UI Thread Congestion (Frame Skipping)**. Soak testing on Samsung A15 hardware revealed significant frame skipping (310+ frames) and "Davey" duration warnings (>1800ms) during Map Hydration (Levels 4-7). Identified the root cause as synchronous disk I/O and database initialization within the OSMDroid `SqlTileWriter` during the first UI update block. Remediated by implementing IO-thread pre-warming of the OSM engine in `GpsApplication` and adding an `isOsmReady` gate to the `LifecycleHydrationManager`. This ensures that heavy map engine initialization occurs off the UI thread and is ready before the hydration sequence proceeds (R758).

## 🟢 Aug.28.07 (vAug.28.07)
*   **Concern #756 Resolved**: **Persistent GNSS/Network Leak remediation**. Hardened `ManagedHardware` with fallback unregistration paths and refactored `CommandRouter` and `SystemStatusProvider` to ensure all network and power receivers are strictly managed via `ManagedBroadcastReceiver` (R756).

---
*For historical entries, see legacy logs.*
