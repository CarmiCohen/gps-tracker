# Handover (July.24.04) - Stealth Hardening & Telemetry Optimization

## 🎯 Current Objective
Cycle **July.24.04** focused on resolving critical regressions: loud alarms in tracker mode (stealth violation), signaling reconnection loops (Issue #540), and startup unresponsiveness (Issue #537).

## 📊 Status Summary

### 1. Resolved: Tracker Stealth Violation (Loud Alarms)
- **Problem**: Loud alarms fired in Tracker mode without a red screen, and no way to stop them.
- **Root Cause**: While `AppAlarmManager` was guarded, `CommandRouter` and UI components triggered `AudioSynthesizer` directly. Additionally, `AppNotificationManager` used `IMPORTANCE_HIGH` channels with default system sounds.
- **Fix**: 
    - Hardened `AudioSynthesizer.kt` with an internal `isTrackerMode` check.
    - Updated `AppNotificationManager.kt` to force `IMPORTANCE_LOW` for all channels and suppress full-screen intents in Tracker Mode.
    - Updated `TrackerService.kt` and `ViewerService.kt` to proactively sync stealth state with the notification manager on startup.

### 2. Resolved: Signaling Rejoin Loop & IPC Congestion (Issue #540)
- **Problem**: Logs showed "Joining room: Trk (Force: true)" repeating every few hundred ms, causing GC pressure.
- **Root Cause**: Identity sync bug in `ConfigManager.kt` where `viewerId` updates were incorrectly overwriting `deviceId`, causing relay rejections. Also, `ConnectivitySuite.kt` lacked a rejoin cooldown.
- **Fix**:
    - Corrected `ConfigManager.kt` identity collectors.
    - Implemented `lastForceJoinTs` cooldown in `ConnectivitySuite.kt` and increased traffic staleness tolerance to 2x `NET_REJOIN_THRESHOLD_MS`.

### 3. Resolved: Memory Churn & Telemetry Flow (Issue #541)
- **Problem**: Continuous Background GC logs (~100ms) during active tracking.
- **Root Cause**: Inefficient `Binary -> Proto -> JSON -> DataClass` path for every incoming packet.
- **Fix**:
    - Expanded `app_settings.proto` (`RealtimeStatus`) with behavioral fields (`is_jammer`, `is_stalled`, `is_tamper_detected`, `jump_tier`).
    - Implemented **Direct Binary Flow**: `SignalingProvider` now dispatches raw bytes via `onBinaryUpdate`, and `ConnectivitySuite.kt` parses them directly into the state repository, bypassing JSON entirely.

### 4. Resolved: Startup Resilience & Landing Page Hang (Issue #537)
- **Problem**: App stuck on landing page until crash.
- **Root Cause**: `MainViewModel` blocked initialization waiting for `appMode` from the repository, and heavy pruning/observations competed with the first UI frame.
- **Fix**:
    - Refactored `MainViewModel.kt` to set `isInitialized = true` immediately after a 200ms settling delay.
    - Decoupled and deferred heavy observations and repository pruning (10s delay).

## ⚠️ Forensic Notes & Risks
- **Logic Restoration**: All files truncated during the previous session (`TrackerService`, `ConnectivitySuite`, `MainViewModel`) have been fully restored with their forensic and stability blocks (Samsung A15 poke logic, GPS audits, ribbons).
- **Proto Sync**: A manual Gradle sync/build is recommended to ensure the updated `RealtimeStatus` Protobuf classes are fully generated.
- **Samsung A15**: The Samsung-specific background "poke" remains active; monitor Logcat for "HEURISTIC RECOVERY" events during long idle periods.

## 🚀 Git Release Procedure
```bash
git add .
git commit -m "release: July.24.04 - enforced stealth, resolved signaling loops, and optimized telemetry flow"
git tag -a July.24.04 -m "July.24.04: Stealth enforcement, IPC loop hardening, and binary telemetry path."
git push origin main --tags
```
