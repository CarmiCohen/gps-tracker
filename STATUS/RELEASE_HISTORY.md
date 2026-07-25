# Project History & Versioning (July.25.02)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## July.25.02 (Forensic Zero-Churn)
- **Forensic Primitive Buffering (#550)**: Refactored `GpsManager` and `AppSensorManager` to use primitive circular arrays (`LongArray`, `DoubleArray`, `BooleanArray`) for high-frequency telemetry history. 
- **Sequence-Based Backfilling**: Optimized forensic retrieval to use sequences, bypassing intermediate list allocations and eliminating heap churn during active tracking.
- **Map Trail Thinning (#548)**: Integrated radial distance pruning (1.0m threshold) to prevent memory bloat during long-duration sessions.

## July.25.01 (Zero-Latency Siren)
- **Reactive Siren Surfacing (#547c)**: Integrated alarm visibility gates directly into the `TelemetryState` stream for immediate UI response.

## July.25.00 (State Decomposition)
- **UI State Decomposition (#547)**: Decomposed monolithic UI state to mitigate kernel-level memory moving overhead on Android 15.

## July.23.11 (Stealth & Startup Hardening)
- **Tracker Stealth Violation (R872)**: Hardened `AppAlarmManager.kt` to suppress siren in tracker mode.
- **FGS Startup Stabilization (R406b)**: Moved `startForeground` to `onCreate` to prevent startup crash loops.

... [See historical logs for full records]
