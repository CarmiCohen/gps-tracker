# Project History & Versioning (Oct16.8)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct16.8 (Chapter 4 Audit - Viewer & Remote Sync)
- **HUD Visibility & Occlusion (#ISSUE-OCT16.8-F3)**: Decoupled `AppMapContainer` from overlay gates, ensuring the map remains visible behind UI surfaces in Portrait mode. (Ch 4.11).
- **Sentinel Z-Index Hardening (#ISSUE-OCT16.8-F4)**: Migrated `AlarmOverlay` to a full-screen `Dialog` via `DialogHost`, ensuring absolute priority over navigation overlays. (R-ID 582, Ch 4.8).
- **Role Transition Grace (#ISSUE-OCT16.8-F2)**: Implemented 30s hardware boot grace in `AppAlarmManager` and reset service start timers during role switches to prevent immediate false-positive alarms. (Ch 4.4).
- **Termination Dialog Fix (#ISSUE-OCT16.8-F1)**: Centralized session termination confirmation in `DialogHost` to prevent occlusion by active overlays.

## Oct16.1 (Native Clamp Unification)
- **Native Clamp Unification (#SIMP-OCT15.12-S1)**: Unified hardware-layer constraints (vibration floors, proximity debouncing) across JNI and JVM using `EngineConstants.h`. (SOT ID 745).
- **Proximity Hardening**: Ported adaptive debouncing and EMA logic to native `n23` fast-path for identical boundary behavior.

## Oct16.0 (Immutable Ribbon Normals)
- **Immutable Ribbon Normals (#SIMP-OCT15.21-S1)**: Moved ribbon normalization to `TelemetryMapper` ingestion point, ensuring analytical data is immutable once emitted. (SOT ID 744).
- **Animation Controller Composition (#SIMP-OCT15.20-S1)**: Centralized Mali GPU anomaly gating via `LocalAnimationGating`.

... (Historical records continue below)
