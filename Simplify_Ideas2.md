# Simplify Ideas (Aug.20.00)

## 1. Unified Cache Management
- **Observation**: `GpsApplication` and `MainRepository` currently manage their own `ShadowCache` instances and clear them independently during memory pressure.
- **Suggestion**: Consider a `GlobalCacheRegistry` or injecting the `ShadowCache` instances through Hilt. This would allow a single point of entry for `onTrimMemory` events to clear all system caches, reducing boilerplate in the `Application` class.

## 2. Trail Point Optimization
- **Observation**: `MainRepository` uses two separate caches for Tracker and Viewer trails.
- **Suggestion**: If memory becomes extremely constrained, these could be unified into a single `ShadowCache<Pair<Long, Boolean>, TrailPoint>` where the Boolean represents the `isViewer` flag.
