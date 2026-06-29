# Project History & Versioning (v8.9.x)

**For historical records (v8.8.x and older), see [docs_history_archive.md](docs_history_archive.md).**

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

## v8.9.7
- **Plunge Matching: Advanced SIT Detection (Issue 196)**: Refined the "Plunge" state machine for forensic parity.
- **SIT Persistence Packet Loss Risk (Issue 194)**: Implemented a reliable, acknowledged event synchronization pipeline.
- **Muzzle Window Hardening (Issue 191)**: Implemented device-specific hysteresis (500ms for A15, 200ms default) during sync I/O.

## v8.9.6
- **Room Migration Forensic Audit (Issue 195)**: Implemented Room migration (v36) with full table reconstruction.
- **Xiaomi Indeterminate Handling (Issue 190)**: Implemented Muzzle/Override logic for Xiaomi "UNKNOWN" autostart status.

## v8.9.5
- **Viewer Background Location (Issue 189)**: Enabled Viewer-side location tracking and relative geofencing.
- **Power Forensic Parity (Issue #337)**: Achieved absolute parity for battery current (`currentMa`) across models and ribbons. (Formerly Issue 192)

## v8.9.4
- **Viewer Engine State Restoration (Issue 187)**: Updated `ViewerService` to load persistent state into `LocationProcessor` on startup.

## v8.9.3
- **Historical GPS Timestamp Preservation (Issue 188)**: Preserved original hardware fix timestamps during backfill.

## v8.9.2
- **Branding Finalization (Issue 183 / R935)**: Migrated to high-resolution JD bitmap icon on brand-aligned green background.
- **GPS Stability Audit (Issue 181)**: Consolidated reliability metrics into 10s intervals.

## v8.9.1
- **Tag Baseline**: Baseline for major version increment.
