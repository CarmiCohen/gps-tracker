# Project History & Versioning (July.30.35)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## July.30.35 (Stability Baseline)
- **Tracker Mode ANR Remediation (#640)**: Implemented aggressive 1000ms throttling and decoupled overlay processing in `MapOverlayManager.kt` to satisfy **R-HARDWARE-01 (Budget Baseline)**.
- **Accuracy Circle Optimization**: Increased drift recalculation threshold to 2.0m and enforced 1s gating to reduce main-thread CPU load on Samsung A15 devices.
- **Version Alignment**: Standardized versioning to July.30.35 across `build.gradle` and all SOT documentation.

## July.30.34 (Maintenance Release)
- **ANR Remediation Initial Implementation (#640)**: Baseline throttling and decoupling logic.

## July.30.32 (Maintenance)
- **Log Spam Remediation (#637)**: Implemented 2000ms short-term status cache for `isLocalOnline()` in `SystemStatusProviderImpl.kt`.
- **Startup ANR Hardening (#639)**: Implemented granular change detection and polygon caching in `MapOverlayManager.kt`.
- **Permission Logic Fix (#638)**: Corrected `PermissionState` defaults in `MainUiState.kt`.

## July.30.31 (Stability)
- **Foreground Service Start Hardening (#634)**: Implemented catch blocks for `ForegroundServiceStartNotAllowedException` in `MainActivity`.

... [See historical logs for full records]
