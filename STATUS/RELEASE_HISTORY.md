# Project History & Versioning (Aug.03.37)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Aug.03.37 (Forensic I/O Hardening)
- **Forensic Spill-Buffer Implementation (#669)**: Implemented `ForensicSpillBuffer` using `MappedByteBuffer` to decouple high-frequency trace capture from SQLite.
- **Zero-Churn Persistence**: Added a background drainer to flush memory-mapped traces in sequential batches, eliminating WAL pressure and "Davey" stalls on budget hardware (R669).
- **Refactoring for Zero-Churn (R668 Consistency)**: Refactored `MainViewModel`, `AnchorEvaluator`, and state models to use in-place mutation and `reset()` patterns, eliminating allocation churn in the hot-path.
- **Forensic Parity**: Standardized `isAdaptiveJump` across signaling and persistence layers.

## Aug.01.10 (Telemetry Churn Hardening)
- **Zero-Churn Telemetry Pipeline (#668)**: Implemented flyweight patterns and object pooling for `SystemHealthState`, `LocationUpdate`, and `ViolationReport` (R-HARDWARE-01).
- **ViewModel Pulse Strategy**: Introduced a `pulse` field to trigger reactive UI updates from mutable state containers, eliminating `copy()` instantiation churn.
- **JNI State Sync Hardening (#667)**: Implemented zero-copy shared buffer path using `DirectByteBuffer` in `MbrainHardwareManager`.
- **Startup Davey Remediation (#664)**: Deferred `osmdroid` and repository initialization by 3000ms to ensure main-thread silence during cold start.

## July.30.35 (Stability Baseline)
- **Tracker Mode ANR Remediation (#640)**: Implemented aggressive 1000ms throttling and decoupled overlay processing in `MapOverlayManager.kt` to satisfy **R-HARDWARE-01 (Budget Baseline)**.
- **Accuracy Circle Optimization**: Increased drift recalculation threshold to 2.0m and enforced 1s gating to reduce main-thread CPU load on Samsung A15 devices.
- **Version Alignment**: Standardized versioning to July.30.35 across `build.gradle` and all SOT documentation.

... [See historical logs for full records]
