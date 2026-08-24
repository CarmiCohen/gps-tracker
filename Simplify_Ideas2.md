# Architectural Simplification Ideas (Aug.22.03)

Following the implementation of UiStateAggregator, Forensic Validation Hooks, and JNI Watchdog hardening, here are remaining recommendations:

## 1. UI State Aggregation (Aug.21.08)
- **Status**: ✅ **RESOLVED**. Extracted to `UiStateAggregator` (Issue #240).

## 2. HUD Parameter Decoupling (Aug.20.10)
- **Simplification**: Further flatten the `StatusBar` hierarchy so child components consume sub-sections of `HudState` directly.

## 3. Dependency Management (Aug.20.10)
- **Simplification**: Move common dependencies into a `Version Catalog` (libs.versions.toml) for consistency across modules.

## 4. Unified Event Bus Reduction
- **Simplification**: Consolidate `UiEvent`, `UiCommand`, and `CommandEvent` into a single reactive `SystemEvent` stream.

## 5. ShadowCache Standardisation
- **Simplification**: Refactor all high-frequency lookups to use the centralized `ShadowCache` to eliminate custom synchronization logic.

## 6. Hydration Tracing (Aug.21.08)
- **Idea #175**: Implement a "Hydration Trace" in the Log Overlay to pinpoint which UI component causes the 1000ms stall on budget hardware (Ref: Issue #248).

## 7. Network Lifecycle Consolidation (Aug.21.08)
- **Idea #176**: Consolidate `ConnectivitySuite` and `SystemStatusProvider` network registrations into a single manager to resolve Issue #253 and reduce OS log noise.

## 8. JNI Background Preload (Aug.21.08)
- **Idea #177**: Use `ZygotePreload` or an async initialization worker for `libjdHardware.so` to move the 81-frame load stall out of the critical startup path (Ref: Issue #265).

## 9. Parallel Aggregation Engine (Aug.21.08)
- **Idea #178**: Parallelize JNI hardware synchronization and HUD state aggregation to mitigate the 1000ms Logic/UI overlap stall identified on A15 hardware.

## 10. Navigation & Native Hardening (Aug.22.00)
- **Idea #179**: Consolidate `MainAppContent` navigation `LaunchedEffect` into a dedicated `AppNavigator` component to reduce Composable complexity and centralize backstack management (Ref: Issue #250).
- **Idea #180**: Extract the JNI execution wrapper (with `withTimeout` and `Mutex` logic) into a generic `NativeSafeExecutor` utility to reuse the watchdog logic across other native modules and reduce boilerplate in `JdHardwareManager` (Ref: Issue #301).

## 11. Unified Database Pruning (Aug.22.03)
- **Idea #181**: Standardize all remaining data tables (`connection_history`, `violations`, `trail_points`) to use the R197 chunked pruning standard. Currently, only `logs` and `pending_status_updates` are fully hardened against I/O stalls.

## 12. Heuristic Muzzling & Identity Trace (Aug.22.08)
- **Idea #182**: Formalize the documentation and diagnostic suppression of OS-level library load heuristics (Ghost Loads) to prevent false-positive forensic investigations (Ref: Issue #251).
