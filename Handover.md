# Handover: GPS Tracker Hardening (July.19.01)

## 🎯 Current Status: July.19.01
The system has been hardened against Main-thread starvation during cold starts and hardware driver failures on Samsung A15 devices. The architecture now strictly enforces staggered initialization and cached IPC property access to ensure UI responsiveness.

## 🟢 Recently Resolved Issues (July.19.01)
1.  **Main Thread Frame Skipping (#099)**:
    - **Problem**: Logcat reported 50+ skipped frames during cold start on low-end hardware.
    - **Root Cause**: Main thread contention between initial UI composition and immediate, redundant IPC calls for permission status.
    - **Resolution**: 
        - Implemented `INITIAL_RENDER_DELAY_MS` (500ms) in `MainViewModel` init to allow rendering to stabilize before starting observations.
        - Hardened `SystemStatusProvider` with lazy caching for hardware flags (`isA15Device`, `isXiaomiDevice`).
        - Moved hardware property checks in `MainActivity.onResume` to the ViewModel's cached state.
    - **Requirement Alignment**: Satisfies R955 (Startup IO Offloading) and R952 (Reactive Setup Flow).

## 🟢 Resolved Issues (July.19.00)
1.  **Hardware Step Detector Registration Failure (#098)**:
    - **Problem**: Samsung A15 sensors returned valid objects but failed `registerListener`, bypassing previous `null` checks.
    - **Resolution**: Added `isStepDetectorRegistered` flag in `AppSensorManager` to detect driver-level failures and automatically trigger the Accelerometer stay-alive fallback (R405).

## 🟢 Resolved Issues (July.18.03)
1.  **Silent Battery Exemption Requirement (#101)**:
    - **Resolution**: Explicitly triggered `PhoneSetupOverlay` on Samsung A15 when battery optimization is enabled.

## 🛠️ System Forensic State (July.19.01)
- **Stay-Alive Strategy**: Authoritative fallback to Accelerometer pulses if Step Detector (Sensor 18) registration fails.
- **Cold-Start Sequence**: 
    1. `loadInitialData` (IO) 
    2. Render Delay (500ms) 
    3. `startBaseObservations` (Permissions/Settings)
    4. `startGlobalTimer` (Heartbeat)
    5. `startHeavyObservations` (Telemetry/History)
- **IPC Throttling**: Permission polling is capped at 15s TTL unless a manual refresh is requested. Hardware model checks are cached for the process lifetime.

## 📂 Authoritative File Registry (Modified in this Session)
- `app/src/main/java/com/gps19/app/AppSensorManager.kt`: Stay-Alive fallback logic.
- `app/src/main/java/com/gps19/app/SystemStatusProvider.kt`: Lazy hardware property caching.
- `app/src/main/java/com/gps19/app/MainViewModel.kt`: Staggered initialization pipeline.
- `app/src/main/java/com/gps19/app/MainActivity.kt`: Hardened `onResume` permission triggers.
- `app/src/main/java/com/gps19/app/MainUiState.kt`: Extended `PermissionState` with hardware flags.
- `STATUS/SOT_MASTER_REQUIREMENTS.md`: Formalized R405c and R955b requirements.

## ⚠️ Known Risks & Residual Tasks
- **Relay Wake-up (#100)**: Intermittent connection timeouts observed during the first wake-up attempt in `AppNetworkManager`. Recommend increasing the initial timeout in the next session.
- **Migration Performance**: Extremely large log tables (>1000 entries) may still cause a noticeable pause on the first launch after a schema update.

## 🚀 Git Release Checklist (Recommended)
1. `git add .`
2. `git commit -m "Release July.19.01: Hardened Cold-Start and Samsung Stay-Alive Fallback"`
3. `git tag -a July.19.01 -m "Architecture Hardening Release"`
4. `git push origin main --tags`

## 🏁 Verification Steps for Next Session
1. Deploy to a Samsung A15.
2. Confirm no "Skipped frames" warnings appear in logcat during launch.
3. Verify that `PhoneSetupOverlay` appears if battery exemption is revoked.
4. Check that stay-alive pulses continue if Step Detector registration fails (logs: `Issue #098: Step Detector exists but registerListener failed`).
