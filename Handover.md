# Project Handover: Samsung A15 Hardening & Cross-Version Signaling (v9.3.25)

## 📌 Status Summary
This document provides a comprehensive forensic snapshot of the project as of **v9.3.25**. The current cycle has optimized the high-frequency telemetry loop via binary Protobuf activation and refined identity conflict logic for better UX.

### 1. Architectural Baseline (v9.3.25)
- **Deployment Device**: Samsung Galaxy A15 (SM-A155F / R58X40GV2AR)
- **Current Role**: Tracker Mode
- **Standardized Heartbeat**: 2000ms (TICK_INTERVAL_MS)
- **Relay Endpoint**: `https://gps-survival-relay.onrender.com`

### 2. Major Resolution: Binary Telemetry Activation (v9.3.25 / R988)
The system now uses an optimized Protobuf-based binary channel for tracker updates, significantly reducing bandwidth and relay CPU overhead.

#### 🛠️ Fixes Implemented
1.  **Protocol Optimization (R988)**:
    - Migrated `trackerState` and `locationPendingReason` from strings to **Protobuf Enums** in `app_settings.proto`.
    - Implemented reverse-mapping in `Models.kt` to unpack binary Enums back to app-wide strings.
    - Activated the binary channel (`location_update_bin`) in `AppNetworkManager.kt` for all tracker transmissions.
2.  **Relay Routing Optimization**:
    - Updated `relay-server/index.js` to support multi-argument `location_update_bin` pulses.
    - Routing now uses an explicit `routingId` provided by the app, allowing the relay to broadcast without parsing Protobuf.
3.  **Identity Hardening (R182b)**:
    - Refined `SettingsRepository.kt` error messages to clarify the reservation of legacy aliases (T/V/Trk/viewer).
4.  **Stability & Bugfixes**:
    - Fixed a compilation error in `CommunicationManager` caused by property naming mismatch (`lastDiscTs`).
    - Fixed a typo in `ConnectionPoint` (`isBatterySteepDischarge`) that broke the history mapping logic.
    - Verified the "Stay-Alive" `TYPE_STEP_DETECTOR` subscription in `AppSensorManager.kt`.

### 3. Active Status
- **Relay Status**: Connected (Supports binary routing).
- **Telemetry Logic**: Trackers emit binary pulses; Viewers support both JSON and Binary.
- **Build Status**: Verified (assembleDebug successful).

### 4. Remaining Tasks for v9.3.26
- [ ] **A15 Stability Audit**: Monitor for "Samsung Suppression" on the A15 during extended (8h+) background sessions with the 2s heartbeat and binary stream.

### 📂 Key Modified Files
- `relay-server`: `index.js`
- `app`: `CommunicationManager.kt`, `AppNetworkManager.kt`, `SettingsRepository.kt`, `SignalingProvider.kt`, `Models.kt`
- `core:engine`: `app_settings.proto`, `SOT_MASTER_REQUIREMENTS.md`
