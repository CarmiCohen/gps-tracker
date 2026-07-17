# Project Handover: July.17.02 - Silence & Persistence Hardened

## 🔴 Status: ENGINE SILENCE ACHIEVED
**Version Context**: `July.17.02` (Authoritative)
**Target Hardware**: Samsung A15 (Budget Benchmark)
**Core Issue Resolved**: #R993 (Notification Spam), #R994 (Unintended Activation)

This document provides the definitive forensic state required to resume development. The system has been hardened to prevent Logcat spam and unintended background execution when the user is on the landing page.

### 1. Forensic Status of Issue #R993 & #R994 (Resolved)
- **Notification Throttling**: Foreground notification updates were decoupled from the 2s engine tick. 
    - **Remediation**: Introduced `NOTIFICATION_THROTTLE_MS` (30,000ms) in `EngineConstants.kt`. Both `ViewerService` and `TrackerService` now respect this interval for UI updates.
- **Unintended Service Start**: The engine previously auto-revived on boot/update if any mode was saved.
    - **Remediation**: Introduced a persistent `isSystemActive` flag in `DataStore`. `BootReceiver` now strictly enforces that the service only restarts if it was manually armed by the user.

### 2. Architectural Updates
- **`isSystemActive`**: New persistent boolean in `AppSettings.proto`. It governs the lifecycle of background services across device restarts.
- **Service Lifecycle**: Services now remain silent and dormant unless the "Armed" state is true.

### 3. Authorized State Management
- **`SessionUseCase`**: Now the authority for toggling system activation state and cleaning up resources.
- **`MainViewModel`**: Orchestrates the UI toggle for activation and ensures persistence through the UseCase.

### 4. Verification Baseline
1. Confirm `NOTIFICATION_THROTTLE_MS` is set to 30000L.
2. Confirm `BootServiceStartWorker` checks both `appMode` and `isSystemActive`.
3. Confirm Version `July.17.02` is displayed.

### 5. Future Simplification Ideas
- **Unified Service**: Merge `TrackerService` and `ViewerService` into a single class using a role-based configuration strategy to reduce duplicate lifecycle logic.
- **Requirement Centralization**: Move all engine-governing constants into a single `EnginePolicy` object.
