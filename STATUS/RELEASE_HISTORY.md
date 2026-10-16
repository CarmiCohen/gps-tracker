# Project History & Versioning (Oct16.1)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct16.1 (Native Clamp Unification)
- **Native Clamp Unification (#SIMP-OCT15.12-S1)**: Unified hardware-layer constraints (vibration floors, proximity debouncing) across JNI and JVM using `EngineConstants.h`. (SOT ID 745).
- **Proximity Hardening**: Ported adaptive debouncing and EMA logic to native `n23` fast-path for identical boundary behavior.

## Oct16.0 (Immutable Ribbon Normals)
- **Immutable Ribbon Normals (#SIMP-OCT15.21-S1)**: Moved ribbon normalization to `TelemetryMapper` ingestion point, ensuring analytical data is immutable once emitted. (SOT ID 744).
- **Animation Controller Composition (#SIMP-OCT15.20-S1)**: Centralized Mali GPU anomaly gating via `LocalAnimationGating`.

## Oct14.1 (Pure Mapping Graduation)
- **Pure Mapping Architecture (#SIMP-OCT14.21-S1)**: Extracted all UI state mapping logic from `MainViewModel` into specialized mappers (`HudMapper`, `DashboardMapper`, `MapStateMapper`). (SOT ID 731).
- **DataStore Hardening (#ISSUE-OCT14.0-F1)**: Migrated settings mutations to functional `updateData` blocks to ensure atomicity.

... (Historical records continue below)
