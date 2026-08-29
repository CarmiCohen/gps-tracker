# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.28.11 (vAug.28.11)
*   **Concern #757 Resolved**: **Persistent BaseEventQueue Leak (Hardening)**. Audit of deployment logs on SM-A155F confirmed residual native resource leaks during service teardown. Identified a lifecycle mismatch where `GpsManager` would skip unregistration if its internal `isStarted` flag was out of sync with active background revival pulses. Hardened `GpsManager` and `AppSensorManager` to perform unconditional unregistration of all GNSS, location, sensor, and display listeners in `stop()`, ensuring deterministic native disposal (R757).
*   **Concern #759 Resolved**: **Logcat Spam Remediation (Hardening)**. Migration of high-frequency permission and capability checks in `SystemStatusProvider` to the `GpsApplication.PACKAGE_NAME` shadow-cache. This eliminated the repetitive `getPackageName` system logs observed during soak testing on the Samsung A15 (R759).

## 🟢 Aug.28.10 (vAug.28.10)
*   **Concern #758 Resolved**: **UI Thread Congestion (Frame Skipping)**. Soak testing on Samsung A15 hardware revealed significant frame skipping (310+ frames) and "Davey" duration warnings (>1800ms) during Map Hydration (Levels 4-7). Identified the root cause as synchronous disk I/O and database initialization within the OSMDroid `SqlTileWriter` during the first UI update block. Remediated by implementing IO-thread pre-warming of the OSM engine in `GpsApplication` and adding an `isOsmReady` gate to the `LifecycleHydrationManager`. This ensures that heavy map engine initialization occurs off the UI thread and is ready before the hydration sequence proceeds (R758).

## 🟢 Aug.28.09 (vAug.28.09)
*   **Concern #757 Resolved**: **Persistent BaseEventQueue Leak (Lifecycle Sync)**. Identified a logic error in `GpsManager.kt` where `stop()` skipped unregistration if the primary location flow hadn't been started, resulting in orphaned background revival callbacks. Refactored the cleanup sequence to be unconditional for all managed resources (R757).

## 🟢 Aug.28.08 (vAug.28.08)
*   **Concern #759 Resolved**: **Logcat Spam Remediation**. Migrated all lookups in `MainActivity` and `BaseMonitorService` to utilize the `GpsApplication.PACKAGE_NAME` shadow-cache (R759).

---
*For historical entries, see legacy logs.*
