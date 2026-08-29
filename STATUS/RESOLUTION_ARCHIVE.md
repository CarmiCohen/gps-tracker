# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.29.00 (vAug.29.00)
*   **Concern #758b Resolved**: **Residual UI Thread Congestion (Async Geometry)**. Soak testing on SM-A155F revealed "Davey" warnings (>1000ms) during Map Hydration (Levels 4-7) despite engine pre-warming. Identified the bottleneck as synchronous point-circle generation in `MapOverlayManager`. Remediated by offloading all circle geometry calculations to `Dispatchers.Default` and implementing an async state-matching pattern to ensure the UI thread remains responsive during heavy overlay updates (R758b).

## 🟢 Aug.28.11 (vAug.28.11)
*   **Concern #757 Resolved**: **Persistent BaseEventQueue Leak (Hardening)**. Audit of deployment logs on SM-A155F confirmed residual native resource leaks during service teardown. Identified a lifecycle mismatch where `GpsManager` would skip unregistration if its internal `isStarted` flag was out of sync with active background revival pulses. Hardened `GpsManager` and `AppSensorManager` to perform unconditional unregistration of all GNSS, location, sensor, and display listeners in `stop()`, ensuring deterministic native disposal (R757).
*   **Concern #759 Resolved**: **Logcat Spam Remediation (Hardening)**. Migration of high-frequency permission and capability checks in `SystemStatusProvider` to the `GpsApplication.PACKAGE_NAME` shadow-cache. This eliminated the repetitive `getPackageName` system logs observed during soak testing on the Samsung A15 (R759).

## 🟢 Aug.28.10 (vAug.28.10)
*   **Concern #758 Resolved**: **UI Thread Congestion (Frame Skipping)**. Soak testing on Samsung A15 hardware revealed significant frame skipping (310+ frames) and "Davey" duration warnings (>1800ms) during Map Hydration (Levels 4-7). Identified the root cause as synchronous disk I/O and database initialization within the OSMDroid `SqlTileWriter` during the first UI update block. Remediated by implementing IO-thread pre-warming of the OSM engine in `GpsApplication` and adding an `isOsmReady` gate to the `LifecycleHydrationManager`. This ensures that heavy map engine initialization occurs off the UI thread and is ready before the hydration sequence proceeds (R758).

---
*For historical entries, see legacy logs.*
