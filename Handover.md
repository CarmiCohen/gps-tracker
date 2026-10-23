# Project Handover - Oct23.121 (Remediation of Violation Persistence & Hysteresis Parity)

## 🟢 Status: COMPLETED (Oct23.121)

Remediation of **ISSUE-OCT23.101-F3** (Stationary Duration Propagation) and **ISSUE-OCT23.101-F4** (Violation Persistence Integrity) is complete. The system now maintains full forensic parity for hysteresis state (`stationaryDurationMs`) and jammer discrimination flags (`isJammingCandidate`) across all signaling, mapping, and persistence layers. Build is stable, and version is incremented to **Oct23.121**.

## ✅ Completed in this Session

### 1. Hysteresis State Parity (ISSUE-OCT23.101-F3)
- **Schema**: Updated `app/src/main/proto/app_settings.proto`. Added `stationary_duration_ms` to `RealtimeStatus` (tag 125) and `TrackerStatusProto` (tag 144).
- **Persistence**: 
    - Extended `LogEntity`, `HistoryEntity`, and `PendingStatusEntity` in `app/src/main/java/com/gps19/app/Database.kt` with `stationaryDurationMs: Long = 0L`.
    - Implemented `MIGRATION_86_87` in `AppDatabase` and bumped Room version to 87.
- **Mapping**: 
    - Hardened `app/src/main/java/com/gps19/app/TelemetryMapper.kt`. Updated `mapProtoToStatus` (line 155), `mapProtoToSnapshot` (line 527), `mapJsonToStatus` (line 309), `mapJsonToSnapshot` (line 621), `mapEntityToApp` (line 778), `mapAppToEntity` (line 826), `mapStatusToPending` (line 873), and `mapPendingToStatus` (line 924) to synchronize `stationaryDurationMs`.
    - Hardened `app/src/main/java/com/gps19/app/TelemetryProtobufMapper.kt` in `mapToRealtime` (line 182), `mapToPersistence` (line 313), and `mapAppToPersistence` (line 379).
- **Engine**: 
    - Added `stationaryDurationMs` to `EngineConnectionPoint` in `core/engine/src/main/java/com/gps19/core/engine/EngineModels.kt` (line 134).
    - Added `stationaryDurationMs` to `SystemHealthState` in `core/engine/src/main/java/com/gps19/core/engine/SystemHealthState.kt` (line 103) and updated the `update()` signature (line 217).

### 2. Violation Persistence Integrity (ISSUE-OCT23.101-F4)
- **UI Model**: Added `isJammingCandidate: Boolean = false` to `ViolationPoint` in `app/src/main/java/com/gps19/app/Models.kt` (line 454) and its `copyFrom`/`contentEquals` logic.
- **Repository**: Updated `MainRepository.addViolation` (line 351) in `app/src/main/java/com/gps19/app/MainRepository.kt` to store the flag in `ViolationEntity`. Updated `violationsFlow` mapping (line 120) to retrieve it.
- **Signaling**: Updated `ConnectivitySuite.emitViolation` (line 678) and the `violation_relay` JSON handler (line 484) in `app/src/main/java/com/gps19/app/ConnectivitySuite.kt` to propagate `is_jamming_candidate` over the network.
- **Forensic Logic**: 
    - Hardened `ServiceForensicUseCase.recordViolationMarkers` and `handleLatch` (line 48) in `app/src/main/java/com/gps19/app/ServiceForensicUseCase.kt` to accept and propagate the flag.
    - Updated `AppAlarmManager.evaluateAlarms` (line 330) in `app/src/main/java/com/gps19/app/AppAlarmManager.kt` to collect the flag from `LocationUpdate.integrity.isJammingCandidate`.

### 3. SOT & Governance
- **Architectural Rules**: Added **R-ID 628** (Hysteresis State Parity) and **R-ID 629** (Violation Integrity Propagation) to `SOT_MASTER_REQUIREMENTS.md`.
- **Build**: Verified success for `:app:assembleDebug`.
- **Baseline**: Created baseline synchronization for version `Oct23.121`.

## 🔴 Current Open Gaps (HIGH)
- **ISSUE-OCT23.101-F6: Offline Violation Event Sync (Low).** Discrete violation markers (emitted via `violation_event`) are not currently buffered for offline synchronization.
- **SIMP-OCT22.0-S1: Settings Validation Centralization (Medium).** Move identity uniqueness and regex validation to a dedicated `ConfigValidator`.

## 🎯 Next Resumption Focus: Remediation of ISSUE-OCT23.101-F6.
Implement a local signaling buffer in `MainRepository` or `OfflineRepository` for `violation_event` payloads. Ensure `signalingProvider.emit` failures for violations trigger persistence and subsequent retry during network recovery.

## 🛠️ Git Release Block
```bash
git add .
git commit -m "Remediation: Resolved ISSUE-OCT23.101-F3 and ISSUE-OCT23.101-F4. Synchronized stationaryDurationMs and isJammingCandidate across all layers for full forensic parity. Oct23.121."
git tag -a Oct23.121 -m "Oct23.121 Remediation Baseline"
git push origin Oct23.121
```

**Current Forensic Metrics (Oct23.121):**
- **[Oct23.121]: [SOT Count: 823 (Rules: 333), Open: H:0, M:1, L:1, Ideas: H:3, M:29, L:47, Testing: 11 (Sub-items: 45), QA: 385]**

**HANDOVER FINALIZED.**
