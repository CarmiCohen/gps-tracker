# Project History & Versioning (Aug.20.00)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Aug.20.00 (Shadow-Cache Hardening)
- **Shadow-Cache Hardening (#217)**: Finalized the LRU-based `ShadowCache` in `core:engine`. Hardened thread-safety for atomic `getOrPut` operations to ensure stability during 100Hz forensic bursts.
- **Archive Restoration**: Restored historical documentation that was truncated during previous documentation sync cycles.

## Aug.19.13 (Systematic JNI & State Consolidation)
- **Systematic JNI Audit (#218)**: Neutralized native identifiers and verified 16KB page-size alignment for Android 15.
- **Atomic Counter Consolidation (#216)**: Unified repository performance counters into `RepositoryMetrics`.

## Aug.19.08 (Advanced Collision Forensic)
- **Samsung CFMS Investigation (#212)**: Concluded forensic audit of vendor-specific SDK load triggers.
- **Battery Exemption Validation (#214)**: Verified R405 safety mechanisms for Samsung hardware.

## Aug.18.13 (Final Release Validation)
- **100Hz Fidelity Verification (#211)**: Confirmed thermal stability and battery performance during real-world moving tests.
- **Log Deduplication Optimization (#210)**: Implemented bit-packed Long signatures for O(N) deduplication efficiency.

## Aug.18.08 (Diagnostic Stress Isolation)
- **UI Bottleneck Remediation (#207/208)**: Eliminated frame hangs via `derivedStateOf` and geometry caching.
- **Permission Fallback (#206)**: Hardened intent-based permission navigation for Samsung API 35.

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

---

## Middle Era Milestones (v9.1.0 - v9.3.9)
- **v9.3.9**: Fixed Peer Visibility (VWR Badge) logic in HUD (#073).
- **v9.3.7**: Baseline documentation synchronization and legacy cleanup.
- **v9.1.7**: System-wide Type Safety Optimization (R999).
- **v9.1.0**: Migration to Protobuf v3 for telemetry serialization.

---

## Foundation Milestones (v8.9.0 - v9.0.0)
- **v9.0.0**: Established Hilt-based Dependency Injection architecture.
- **v8.9.91**: Optimized osmdroid storage paths and static user-agent configuration.
- **v8.9.0**: Initial implementation of the Location Sentinel and Bayesian uncertainty logic.
