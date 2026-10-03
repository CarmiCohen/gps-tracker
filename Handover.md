# Forensic Handover (Oct.3.6 - CONNECTIVITY & HUD OPTIMIZATION)

## 🎯 Current System State
*   **Version**: `Oct.3.6` | **Status**: 🟢 **OPERATIONAL**.
*   **Connectivity Hardening (Issue #1422)**:
    *   **WebSocket Priority**: Forced `websocket` transport in `CommunicationManager.kt` to resolve Render.com long-polling timeouts.
    *   **Reactive Re-binding**: `MonitorService.kt` now uses a `combine` flow to observe `relayUrl`, `deviceId`, and `viewerId`, triggering immediate reconnection on change.
    *   **Validator Fix**: Corrected inverted logic in `SignalingValidator.shouldProcessLogRelay` that was dropping tracker logs on the viewer.
    *   **Permissions**: Added `ACCESS_NETWORK_STATE` to `AndroidManifest.xml` to enable reliable handover between Wi-Fi and Data.
*   **HUD Optimization (Issue #1421)**:
    *   **Consolidation**: Merged badge row and telemetry row in portrait mode to reduce vertical footprint by ~40%.
    *   **Transparency**: Reduced HUD surface alpha to `0.4` for map visibility.
    *   **De-confliction**: Header and HUD now hide when Settings/Logs overlays are active.
*   **JNI Integration**: Native stationary convergence (`n12`/`n13`) and pulse auditing are active.

## 🟢 Audit Record
*   **Build Status**: 🟢 **SUCCESSFUL**. Verified via `:app:assembleDebug`.
*   **Integrity Audit**: Connectivity parameters are now reactive; UI occlusion resolved.
*   **Traceability**: SOT IDs 604, 605 / Rules 1.96, 1.97, 1.98 established.

## 🚀 Resumption Action Path (Next Chat)
1.  **Reactive Siren Lockout (Issue #1201)**:
    *   Decouple siren cooldown logic from audio generation into a domain UseCase.
2.  **UI Event Routing (Issue #1202)**:
    *   Consolidate navigation and global UI commands into a single coordinator to decouple ViewModels from Compose.
3.  **Smart Signaling Dispatcher (Issue #1172)**:
    *   Complete the migration of all signaling triggers to the `SmartSignalingDispatcher`.

## 🧪 Latest Bug Test Procedure
*   **Version Check**: Verify footer text shows `Oct.3.6`.
*   **SRV Badge**: Verify SRV turns Green within 3s of saving valid Relay URL.
*   **Log Relay**: In Viewer mode, open Logs and verify "REMOTE" entries from the tracker are visible.
*   **HUD Transparency**: Verify map is clearly visible behind the status row in portrait.
*   **Settings Access**: Verify settings menu is fully accessible without overlapping badges.

---

## 📊 Hardening Progress Dashboard (Oct.3.6)
- **Oct.3.6: [SOT Count: 261 (Rules: 120), Open: H:0, M:0, L:0, Ideas: H:0, M:5, L:3, Testing: 24, QA: 371]**
- **Audit Record**: Connectivity sticky-state resolved; HUD visibility optimized; Manifest aligned.
