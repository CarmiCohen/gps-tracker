# Simplification Ideas 2.0 (Aug.31.02)

*   **History Sampling (R650) - COMPLETE**: Hardened the ribbon rendering pipeline by integrating `sample()` in `MainViewModel.kt`. This eliminates Davey stalls on budget hardware (A15) by decoupling high-frequency DB writes from UI recomposition.
*   **Forensic Metadata Sanitization (R779) - COMPLETE**: Implemented `ForensicSanitizer` to scrub absolute internal paths and normalize hardware identifiers at the logging edge.
*   **ManagedHardware Ecosystem (R750-R757) - COMPLETE**: Implemented specialized lifecycle-aware wrappers for deterministic native resource cleanup.
*   **Fallback Unregistration Hardening (R767/R775) - COMPLETE**: Added synchronous direct unregistration paths in `ManagedHardware`. 
*   **Map Hydration Gating (R758) - COMPLETE**: Resolved startup ANRs by offloading heavy OSMDroid engine initialization.
*   **Async Geometry Generation (R758b) - COMPLETE**: Offloaded circle/geofence point calculations to `Dispatchers.Default`.
*   **Segmented Overlay Population (R776/R777) - COMPLETE**: Applied segmented coroutine pattern with `yield()`.
*   **Unified Hardware Registry (R760) - COMPLETE**: Consolidated legacy managers into `HardwareProvider`.
*   **Stationary Transparency (R765) - COMPLETE**: Integrated definitive `[ULTRA]` visual indicators.
*   **MainViewModel Boilerplate**: Evaluate consolidating the 6 history scale flows (4M, 1H, etc.) into a single map-based StateFlow if the UI can be refactored to consume a keyed subscription.
*   **Forensic Replay Sanitization**: Evaluate if the Replay engine should use the same `ForensicSanitizer` logic when scrubbing coordinates or other technical metadata before rendering to external screens.
