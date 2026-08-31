# Project History & Versioning (Aug.31.04)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Aug.31.04 (Forensic Replay Hardening)
- **Forensic Replay & Metadata Hardening (#779)**: Extended `ForensicSanitizer` policy to the telemetry mapping and historical audit layers. Scrubbed `net_interface` and audit logs at the source to ensure no hardware-bound identifiers leak during historical replay or export.

## Aug.31.03 (Ultra-Long Stationary State Hardening)
- **Ultra-Long Stationary State (#762)**: Hardened end-to-end propagation of `isUltraLongStationary` across `IntegrityMonitor`, `TelemetryUseCase`, and `HistoryManager`. Ensured definitive `[ULTRA]` badge transparency.

## Aug.31.02 (UI Performance Hardening)
- **History Sampling Authority (#782)**: Hardened forensic ribbon flows in `MainViewModel` with Samsung A15-specific 3000ms sampling windows to ensure Davey immunity during stress tests (R650).

## Aug.31.00 (Binary Protocol Expansion)
- **Binary Protocol Expansion (#782b)**: Expanded `RealtimeStatus` Protobuf schema and implemented database v75 migration to carry violation metrics in hot-path telemetry.

## Aug.30.13 (Forensic Metadata Sanitization)
- **Forensic Metadata Sanitization (#779)**: Implemented centralized `ForensicSanitizer` to scrub absolute internal paths and normalize hardware identifiers in exported logs and JSON snapshots.

## Aug.20.00 (Shadow-Cache Hardening)
- **Shadow-Cache Hardening (#217)**: Finalized the LRU-based `ShadowCache` in `core:engine`. Hardened thread-safety for atomic `getOrPut` operations.
- **Archive Restoration**: Restored historical documentation that was truncated during previous documentation sync cycles.

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
