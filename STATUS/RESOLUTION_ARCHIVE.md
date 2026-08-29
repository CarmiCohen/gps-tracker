# Resolution Archive

This document archives all resolved issues and architectural refinements.

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

---
*For historical entries, see legacy logs.*
