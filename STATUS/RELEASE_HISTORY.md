# Project History & Versioning (Aug.03.50)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Aug.03.50 (Forensic Recovery Integrity)
- **Forensic Recovery Integrity Validation (#703)**: Hardened `ForensicSpillBuffer` with Magic Number validation, circular buffer header sanity checks, and CRC32 checksums for every entry.
- **Zero-Allocation CRC**: Optimized checksum calculation for API 24+ compatibility without heap churn, ensuring forensic integrity even after sudden system power loss (R703).

## Aug.03.47 (Trace Serialization Hardening)
- **Trace Serialization Hardening (#702)**: Implemented full binary serialization for the forensic hot-path. Raw telemetry is now serialized directly to `MappedByteBuffer`.
- **Deferred Message Formatting**: Moved human-readable string reconstruction to the background drainage phase, eliminating StringBuilder churn in the 100Hz loop (R702).

## Aug.03.46 (Spatial Quantization)
- **Forensic Spatial Quantization (#701)**: Implemented 0.1m displacement gating for forensic traces, suppressed during stationary periods unless IMU delta thresholds (vibration/tilt) are exceeded (R701).

## Aug.03.45 (Sampling Scaling)
- **Power-Aware Sampling (#700)**: Implemented dynamic forensic sampling (10Hz - 100Hz) based on charging and thermal states. Integrated zero-allocation logging path (R700).

## Aug.03.37 (Forensic I/O Hardening)
- **Forensic Spill-Buffer Implementation (#669)**: Implemented `ForensicSpillBuffer` using `MappedByteBuffer` to decouple high-frequency trace capture from SQLite.
- **Zero-Churn Persistence**: Added a background drainer to flush memory-mapped traces in sequential batches, eliminating WAL pressure and "Davey" stalls on budget hardware (R669).
- **Refactoring for Zero-Churn (R668 Consistency)**: Refactored `MainViewModel`, `AnchorEvaluator`, and state models to use in-place mutation and `reset()` patterns, eliminating allocation churn in the hot-path.

... [See historical logs for full records]
