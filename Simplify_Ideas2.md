# Simplification Ideas 2.0 (Aug.28.10)

*   **ManagedHardware Ecosystem (R750-R757) - COMPLETE**: Specialized wrappers for Network, Location, GNSS, Sensors, and Broadcast receivers ensure deterministic native resource cleanup.
*   **Map Hydration Gating (R758) - COMPLETE**: Offloaded heavy OSM engine initialization to a background IO thread and used a thread-safe `AtomicBoolean` gate (`isOsmReady`) to prevent Main thread stalls during startup hydration.
*   **Unified Hardware Registry**: Consider merging `GpsManager` and `AppSensorManager` into a single `HardwareProvider` now that unregistration logic is unified in `ManagedHardware.kt`.
*   **IPC Identification (R759)**: Continue identifying system-level lookups (UID, ProcessInfo) that might be triggering hidden OS diagnostic spam on restricted hardware and migrate them to the `GpsApplication` shadow-cache.
*   **Map Hydration Staggering Refinement**: Evaluate if Levels 5-7 (Overlays) can be moved to `IdleHandler` callbacks as well, or if the current delay-based staggering is optimal for frame consistency.
