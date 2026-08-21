# Architectural Simplification Ideas (Aug.21.08)

Following the implementation of UiStateAggregator and Forensic Validation Hooks, here are remaining recommendations:

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
