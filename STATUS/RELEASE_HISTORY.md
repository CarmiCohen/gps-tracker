# Project History & Versioning (OCT15.2)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## OCT15.2 (Primitive History Buffers)
- **History Buffer Optimization (#SIMP-OCT15.1-S1)**: Transitioned high-frequency connection history telemetry from object-heavy lists to a memory-efficient `HistoryBuffer` backed by primitive arrays (`LongArray`, `DoubleArray`). (SOT ID 733).
- **GC Pressure Mitigation**: Reduced object allocations per emission from O(N) to O(1), eliminating telemetry-driven UI stalls on budget hardware.

## OCT15.1 (Trail Buffer Optimization)
- **Trail Buffer Optimization (#SIMP-OCT15.0-S1)**: Refactored trail flows to use `TrailBuffer` (primitive arrays), reducing object overhead from O(N) to O(1) per emission. (SOT ID 732).
- **Search Optimization**: Refactored `findClosestTrailPoint` to use binary search on primitive timestamp arrays.

## Oct14.1 (Pure Mapping Graduation)
- **Pure Mapping Architecture (#SIMP-OCT14.21-S1)**: Extracted all UI state mapping logic from `MainViewModel` into specialized mappers (`HudMapper`, `DashboardMapper`, `MapStateMapper`). (SOT ID 731).
- **DataStore Hardening (#ISSUE-OCT14.0-F1)**: Migrated settings mutations to functional `updateData` blocks to ensure atomicity.

... (Historical records continue below)
