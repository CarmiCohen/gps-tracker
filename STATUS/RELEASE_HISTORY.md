# Project History & Versioning (July.20.07)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## July.20.07
- **Golden Master Release**: Finalized release hardening and migrated authoritative state to `main`.
- **Hilt Migration**: Completed transition to Hilt-managed `@ApplicationScope` and `@HiltViewModel`.
- **Step Detector Authority (#107)**: Implemented full permission lifecycle and hardware registration health checks.
- **Startup Performance (#109/111/115)**: Offloaded I/O tasks and decoupled pruning from the UI path. Startup jank <200ms.
- **ViewerService Restoration (#117)**: Resolved compilation errors in alarm evaluation logic.

## July.20.01
- **Forensic Ribbon Continuity Verification (#105)**: Reconstructed monotonic timeline on startup using bridged `rt` timestamps. Hardened initialization to preserve forensic continuity across process boundaries.

## July.20.00
- **Startup Hardening (#104)**: Integrated proactive database pruning in `MainViewModel.loadInitialData` to prevent startup ANRs on low-end hardware.

## July.18.01 (v9.3.57)
- **Room Identity Hash Stabilization (#097)**: Resolved `IllegalStateException` integrity error on version 56 by adding `MIGRATION_56_57`.
- **Version Baseline**: Updated versioning to July.18.01 across documentation.

## v9.3.19
- **Version Alignment**: Updated system-wide versioning to July.16.22.

## v9.3.18
- **Startup ANR Remediation (R403)**: Implemented dynamic heartbeat recovery logic (2s heartbeat during startup).
- **Relay Configuration Authority (R404)**: Unified relay URLs to fallback to `MainRepository.DEFAULT_RELAY_URL`.
- **Forensic Visual Standardization (R404b)**: Synchronized `FORENSIC_PINK_COLOR` (#FF1493) across modules.
