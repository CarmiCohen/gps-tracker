# Project History & Versioning (v9.0.x)

**For historical records (v8.9.x and older), see [docs_history_archive.md](docs_history_archive.md).**

## v9.0.3
- **Viewer Status Restoration (Issue #029)**: Fixed a bug where the Viewer's own status line was grayed out in the UI. Updated `ViewerService.kt` to propagate local `LocationUpdate` packets to the repository, ensuring the `DAT` badge and telemetry timestamps are kept fresh.

## v9.0.2
- **GPS Stability Audit (R951)**: Implemented a monotonic reliability audit for Viewer-side GPS fixes. Detects and logs stability gaps during high-frequency polling.
- **Polling Standard**: Standardized `VIEWER_GPS_POLLING_MS` to 1000L for increased local responsiveness.

## v8.9.99
- **Identity Sanitization (Issue #041)**: Implemented strict alphanumeric Regex validation for all IDs. Hardened services to reject malformed peer pulses.

---

## v8.9.78
- **Stationary Anchor Hard-Lock (Issue #018)**: Implemented "Hard-Lock" coordinate clamping in `LocationProcessor.kt` to eliminate GPS drift (spaghetti trails) when stationary confidence > 0.9.
- **Persistence Schema v52**: Incremented DB schema to include `isAnchorLocked` flag in forensic history.
- **Android 14+ FGS Resilience (Issue #019)**: Implemented "Recent UI Pulse" window (15s) to authorized background-to-foreground transitions for sensitive types (Microphone).

## v8.9.75
- **System-Wide Type Safety (Issue #014)**: Completed full refactor of telemetry chain to native `Double` types across `:core:engine`, `:app`, and Room persistence. Eliminated conversion overhead for Accuracy, Speed, and Bearing.

## v8.9.72
- **Coroutine Resilience (Issue #015)**: Hardened `SyncManager` and `CommandRouter` to silently handle `CancellationException` during service lifecycle transitions.

## v8.9.60
- **Forensic Ghost Mode Restoration (Issue #458)**: Fixed a bug where Tracker-role devices showed local status as stale. Corrected timestamp propagation in `GlobalStatusBar`.
- **Telemetry Freshness Logic (Issue #460)**: Relaxed `isLocalTelemetryFresh` check to support sensor-only telemetry existence, preventing "Ghost Mode" before the first GPS fix.
- **Unicode Label Fix (Issue #459)**: Corrected double-escaped thin-space characters in `StatusBar` and `GlobalStatusBar` labels.

## v8.9.54
- **Jitter-Proof UI Staleness (Issue #427/428)**: Relaxed "Ghost Mode" and Watchdog staleness thresholds to **15s** to accommodate network jitter and prevent UI flickering. (Supersedes previous 10s mandate).

## v8.9.38
- **Hardening Phase Completion**: Finalized audit of all 58 resolutions in the hardening phase.
- **SIT Rising-Edge Guard**: Implemented physical-event latches to prevent duplicate forensic logs.
- **Samsung A15 GPS Stabilization**: Enforced 1000ms polling and active WakeLock renewal for 100% background persistence.
- **Xiaomi MIUI 14 Resilience**: Implemented indeterminate status handling and 30s boot grace period.
- **Trajectory Gating**: Unified jitter suppression and outlier rejection logic in `PhysicsUtils`.

## v8.9.10
- **Log Spatial Anchor (Issue #208)**: Implemented coordinate auto-population in `LogManager`. All forensic logs and critical alerts are now automatically anchored with `lat`/`lng` coordinates using the last known telemetry position.
- **Documentation Hardening**: Synchronized `REQUIREMENTS_SOT.md`, `info-elementary-fields.md`, and `issues.md` to the v8.9.10 baseline.
- **Xiaomi Resilience Verification**: Verified that `XIAOMI_BOOT_GRACE_MS` (30s) logic in `MainAlarmLogic.kt` correctly suppresses transient alarms during system startup.

## v8.9.9
- **Documentation Synchronization (Issue 203)**: Synchronized all spec docs to the v8.9.9 logic baseline.
- **Threshold Hardening (Issue 204/205)**: Aligned `REQUIREMENTS_SOT.md` with `EngineConstants.kt`. Hardened GPS Stall (60s), Revival (120s), and Muzzle Window (2000ms) thresholds.
- **UI Staleness Unification (Issue 206)**: Unified "Ghost Mode" and "Position Health" thresholds to 10s. (Note: Relaxed to 15s in v8.9.54).

## v8.9.8
- **Room Migration Registry Fix (Issue 200)**: Registered `MIGRATION_37_38` in `AppModule.kt`.
- **Zombie Telemetry UX Sweep (Issue 193)**: Applied `Slate500` ("Ghost Mode") dimming to all stale forensic fields (>10s baseline at the time).
- **Aggressive Stall Recovery (Issue 198)**: Shortened GPS stall detection to 60s and revival retry to 120s.
- **Build Modernization (Issue 199)**: Upgraded project toolchain to Java 17 and Android SDK 35.

## v8.9.3
- **Historical GPS Timestamp Preservation (Issue 188)**: Preserved original hardware fix timestamps during backfill.

## v8.9.2
- **Branding Finalization (Issue 183 / R935)**: Migrated to high-resolution JD bitmap icon on brand-aligned green background.
- **GPS Stability Audit (Issue 181)**: Consolidated reliability metrics into 10s intervals.

## v8.9.1
- **Tag Baseline**: Baseline for major version increment.
