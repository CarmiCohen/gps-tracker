# Architectural Simplification Ideas (vAug.20.07)

## 1. Telemetry Mapping Consolidation (Issue #225)
- **Current State**: Mapping between `HistoryEntity`, `PendingStatusEntity`, and `ConnectionPoint` is duplicated across several extension functions and repository methods.
- **Idea**: Create a unified `ForensicMapper` interface or utility object that handles the 1:1 parity transformation for all high-frequency metadata (sitVzTs, sitVzRt, etc.). This ensures that adding a new forensic field only requires a change in one location.

## 2. HUD State Centralization
- **Current State**: Freshness and validity states for GPS and IMU are derived in multiple ViewModels.
- **Idea**: Centralize the "System Health" logic into a single `ForensicStateMonitor` that emits a unified `TelemetryHealth` object. This would allow `derivedStateOf` to act on a single source of truth, reducing recomposition triggers in the HUD.

## 3. Room Database Migration Automation
- **Current State**: Manual migration scripts (`MIGRATION_72_73`) are becoming complex.
- **Idea**: Evaluate the use of `AutoMigration` for simple field additions, keeping manual scripts only for complex index reconstructions or data sanitization.

## 4. Shadow-Cache Primitive Pooling
- **Current State**: `ShadowCache` stores `TrailPoint` objects.
- **Idea**: Explore using primitive arrays for coordinate history to further reduce heap pressure and GC churn during 100Hz tracking sessions.
