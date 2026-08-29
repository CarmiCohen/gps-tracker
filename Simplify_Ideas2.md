# Simplification Ideas 2.0 (Aug.29.10)

*   **ManagedHardware Ecosystem (R750-R757) - COMPLETE**: Specialized wrappers for Network, Location, GNSS, Sensors, and Broadcast receivers ensure deterministic native resource cleanup.
*   **Map Hydration Gating (R758) - COMPLETE**: Offloaded heavy OSM engine initialization to a background IO thread.
*   **Unified Hardware Registry (R760) - COMPLETE**: Merged `GpsManager` and `AppSensorManager` into a single `HardwareProvider`.
*   **Engine Config Unification (R764) - COMPLETE**: Removed redundant `DeviceSpecialFlags` and migrated logic to use engine-level `HardwareCapabilities` directly.
*   **IPC Identification (R759)**: Continue identifying system-level lookups (UID, ProcessInfo) that might be triggering hidden OS diagnostic spam on restricted hardware and migrate them to the `GpsApplication` shadow-cache.
*   **Map Hydration Staggering Refinement**: Evaluate if the current delay-based staggering in `LifecycleHydrationManager` is still necessary given the async geometry offloading, or if it can be simplified.
*   **[Acoustic Logic]**: Encapsulate adaptive duty-cycle calculation into a standalone pure function within `HardwareProvider` to reduce complexity in the monitor thread loop (R762b).
*   **[Stationary Derivation]**: Evaluate if "Ultra-Long" state can be derived entirely on the Viewer side using the monotonic `rt` (uptime) property of coordinates, potentially removing the need for a dedicated `isUltraLongStationary` flag in the telemetry payload.
