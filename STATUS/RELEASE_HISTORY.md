# Project History & Versioning (Oct12.12)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct12.12 (Forensic Integrity & Batch Persistence)
- **Forensic Integrity (#SIMP-1012-3)**: Integrated CRC32 checksums into `ForensicSpillBuffer.kt` and `LogEntity` to detect storage-level corruption. Incremented DB version to 83. (SOT ID 680).
- **Batch Persistence (#SIMP-1012-4)**: Implemented background batch-flush loop in `OfflineRepository.kt` to reduce SQLite write-lock contention during high-frequency telemetry bursts. (SOT ID 681).

## Oct12.2 (Network Hardening & GNSS Burst Refining)
- **Network Buffer Hardening (#SIMP-1012-2)**: Migrated `SmartSignalingDispatcher.kt` to bounded channels with `DROP_OLDEST` and introduced `SignalingPriority.CRITICAL`. (SOT ID 678).
- **GNSS Burst Refining (#SIMP-1107-2)**: Dynamically scaled radio-warm keep-alive duration in `HardwareSuite.kt` based on RTT stability (4s/10s). (SOT ID 679).

## Oct12.1 (Forensic Buffer Optimization)
- **Forensic Sub-sampling (#SIMP-1012-1)**: Implemented vibration sub-sampling (1/2 rate) in `HardwareSuite.kt` and aggressive buffer trimming during memory pressure. (SOT ID 677).

## Oct11.2 (Authoritative Sensor & JNI Hardening)
- **Acoustic Floor Hardening (#SIMP-1011-10)**: Implemented temporal baseline decay (Leaky Integrator) in `jdhardware-jni.cpp` and `JdHardwareManager.kt`. (SOT ID 674).
- **JNI Resource Audit (#SIMP-1011-11)**: Hardened `n22` with explicit array bounds verification and zero-JNI loops. (SOT ID 675).
- **HUD Throttling Sync (#SIMP-1011-12)**: Integrated `HUD_STATE_SAMPLE_THROTTLED_MS` (1000ms) in budget hardware scenarios. (SOT ID 676).

---
*For historical entries, see [docs_history_archive.md](docs_history_archive.md) or Git logs.*
