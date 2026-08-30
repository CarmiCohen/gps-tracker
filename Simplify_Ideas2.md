# Simplification Ideas 2.0 (Aug.29.13)

*   **ManagedHardware Ecosystem (R750-R757) - COMPLETE**: Specialized wrappers for Network, Location, GNSS, Sensors, and Broadcast receivers ensure deterministic native resource cleanup.
*   **Map Hydration Gating (R758) - COMPLETE**: Offloaded heavy OSM engine initialization to a background IO thread and used a thread-safe `AtomicBoolean` gate (`isOsmReady`) to prevent Main thread stalls during startup hydration.
*   **Async Geometry Generation (R758b) - COMPLETE**: Offloaded circle and geofence point calculations to background threads to ensure Main-thread fluidity (R758b).
*   **Unified Hardware Registry (R760) - COMPLETE**: Merged `GpsManager` and `AppSensorManager` into a single `HardwareProvider`.
*   **Engine Config Unification (R764) - COMPLETE**: Removed redundant `DeviceSpecialFlags` and migrated logic to use engine-level `HardwareCapabilities` directly.
*   **Acoustic Logic Encapsulation (R762b) - COMPLETE**: Successfully encapsulated adaptive duty-cycle calculation into a standalone pure function in `SentinelValidator.kt`, significantly reducing complexity in the `HardwareProvider` monitoring loop.
*   **Stationary Transparency (R765) - COMPLETE**: Integrated `[ULTRA]` badges for hardware-state transparency.
*   **Technical UI Directionality (R766) - COMPLETE**: Enforced LTR directionality for telemetry components, simplifying layout management across diverse locales.
*   **IPC Identification (R759)**: Continue identifying system-level lookups (UID, ProcessInfo) that might be triggering hidden OS diagnostic spam on restricted hardware and migrate them to the `GpsApplication` shadow-cache.
*   **Map Hydration Staggering Refinement**: Evaluate if the current delay-based staggering in `LifecycleHydrationManager` is still necessary given the async geometry offloading, or if it can be simplified.
*   **[Stationary Derivation]**: Evaluate if "Ultra-Long" state can be derived entirely on the Viewer side using the monotonic `rt` (uptime) property of coordinates, potentially removing the need for a dedicated `isUltraLongStationary` flag in the telemetry payload.
