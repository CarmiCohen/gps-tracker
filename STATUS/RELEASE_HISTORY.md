# Project History & Versioning (Oct13.4)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## Oct13.4 (Full Build & Baseline Verification)
- **Full Build Execution**: Successfully executed comprehensive assembly and unit test suite (72/72 passed).
- **Baseline Synchronization**: Synchronized `SOT_MASTER_REQUIREMENTS.md` and `issues.md` for the `Oct13.4` hardening baseline.

## Oct13.3 (Persistence & Power Hardening)
- **Sequential Persistence (#ISSUE-OCT13.3-1)**: Refactored `OfflineRepository` to use a Channel-based actor to prevent data loss during high-frequency bursts. (SOT ID 707).
- **Doze Hardening (#ISSUE-OCT13.3-3)**: Updated `PowerIntegrationAuditTest` with iterative stepping to ensure reliable idle state detection. (SOT ID 706).
- **Mapping Hardening (#SIMP-OCT11.20-S1)**: Verified EMA alpha branches and coordinate smoothing with high-fidelity test data. (SOT ID 705).
- **Unified HUD (#SIMP-OCT12.16-S1)**: Merged HeaderBar, Status, and Map overlays into a single `OverlayManager`. (SOT ID 704).

## Oct12.12 (Forensic Integrity & Batch Persistence)
- **Forensic Integrity (#SIMP-1012-3)**: Integrated CRC32 checksums into `ForensicSpillBuffer.kt` and `LogEntity` to detect storage-level corruption. Incremented DB version to 83. (SOT ID 680).
- **Batch Persistence (#SIMP-1012-4)**: Implemented background batch-flush loop in `OfflineRepository.kt` to reduce SQLite write-lock contention during high-frequency telemetry bursts. (SOT ID 681).

## Oct12.2 (Network Hardening & GNSS Burst Refining)
- **Network Buffer Hardening (#SIMP-1012-2)**: Migrated `SmartSignalingDispatcher.kt` to bounded channels with `DROP_OLDEST` and introduced `SignalingPriority.CRITICAL`. (SOT ID 678).
- **GNSS Burst Refining (#SIMP-1107-2)**: Dynamically scaled radio-warm keep-alive duration in `HardwareSuite.kt` based on RTT stability (4s/10s). (SOT ID 679).

...
