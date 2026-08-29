# Resolution Archive

This document archives all resolved issues and architectural refinements.

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

---
*For historical entries, see legacy logs.*
