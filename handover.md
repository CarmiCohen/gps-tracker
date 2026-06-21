# Project Handover - v8.9.14 Telemetry Hardening

## 1. Context Summary
- **Project**: `gps-tracker` (Native Android, Kotlin/Compose).
- **Current Baseline**: **v8.9.14** (Phase 3: Telemetry & Audit Optimization Complete).
- **Architecture**: Clean Architecture with modular separation. 
- **Toolchain**: Java 17 and Android SDK 35 (Android 15) confirmed.

## 2. Completed in v8.9.14 (Phase 3)
### Telemetry & Audit Optimization (Issues #211, #212)
- **Throttle Stability Audit (#211)**: Hardened gating logic in `TrackerService.processTick`. "STABILITY AUDIT" logs now only emit if `reliability < 98%` or `gpsMaxGapMs > 200ms`. This significantly reduces log clutter on high-frequency devices while maintaining forensic visibility into actual performance regressions.
- **Enhanced Recovery Logic (#212)**: Hardened the historical reconstruction pipeline in `RemoteHandler.handleRemoteLog` to prioritize the persisted `accuracy` field from `LogEntry`. Reconstructed forensic markers now reflect their original spatial precision.
- **Log Manager Accuracy Hardening**: Updated `LogManager.submitToLogSink` to ensure `accuracy` is intelligently populated from telemetry even when coordinates are explicitly provided without a companion accuracy value.

## 3. Pending Tasks
1.  **Issue #190: MIUI 14 Field Verification**:
    - Awaiting physical hardware verification of the boot grace period.

## 4. Key Files for Reference
- `issues.md`: Master tracking of all fixed and open issues.
- `core/engine/src/main/java/com/gps19/core/engine/EngineConstants.kt`: Centralized stability thresholds.
- `app/src/main/java/com/gps19/app/LogManager.kt`: Hardened accuracy propagation.
- `app/src/main/java/com/gps19/app/RemoteHandler.kt`: Accuracy-aware historical recovery.
