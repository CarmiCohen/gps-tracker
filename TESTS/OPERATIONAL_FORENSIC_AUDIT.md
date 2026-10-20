# Operational Forensic Audit - GPS Tracker (vOct20.33)

**Goal:** Verify system health via runtime telemetry and log signatures during deployment. This chapter contains high-frequency audit points that must be verified for every production-ready build.

---

## 0.1 Logcat Anomaly Monitoring
*   **Action:** Deploy the app and filter Logcat for `StackLog`, `Davey`, or `Exception`.
*   **Verification:** Confirm zero `StackLog` leaks during steady-state operation. Monitor for `Davey` events > 500ms during 100Hz sensor bursts.

## 0.2 Event Log Continuity
*   **Action:** Observe "Stay-alive pulse" and "PRUNING" signatures in Logcat.
*   **Verification:** Confirm pulses trigger every 30s. Confirm maintenance tasks execute without blocking the main thread.

## 0.3 Tracker Stealth & Authority Audit (Issue #Oct20.31-F1)
*   **Action:** Deploy the app in Tracker Mode and inspect the UI, HUD, and Settings.
*   **Verification:** **STEALTH BALANCE (Updated Oct20.33)**. 
    1. Confirm HUD contains NO **ALM** badge or **ALARM** indicators.
    2. Confirm HUD contains NO **LOCK** or interactive alarm control buttons.
    3. Confirm HUD contains NO **SIREN** indicators or audio activity.
    4. **Settings Gear (RESTORED)**: Confirm settings gear is visible to allow geofence (fence) state verification.
    5. Confirm Map contains NO **Home Point** management tools (ADD/DEL).
    6. Tracker must remain a pure telemetry provider; all logic/alarm authority resides exclusively on the Viewer.

## 0.4 Back Button Lifecycle (Issue #1424)
*   **Action:** Tap the system "Return/Back" button while on the Viewer or Tracker screen.
*   **Verification:** Confirm the app DOES NOT close or cease operations. It should either stay on the current screen (if it's the root) or navigate back correctly without process termination.

## 0.5 UI Alignment & Occlusion Audit (Issue #1425, #1426, #OCT20.33-F2)
*   **Action:** Inspect the Viewer UI in various orientations.
*   **Verification:** 
    1. **Version Position**: Confirm the version string (e.g., `Oct20.33`) is at the absolute bottom-start edge and does NOT overlap with the Map Scale bar.
    2. **HUD Visibility**: Confirm the upper HUD header has a Slate900 scrim.
    3. **HUD Compression**: Verify the telemetry HUD is NOT squeezed and buttons have proportional padding.
    4. **Debug Suppression**: Confirm `UI_READY` label is suppressed in PLATINUM builds.

## 0.6 Peer Sync & Connectivity Audit (Issue #1427, #1428, #OCT20.33-F1)
*   **Action:** Connect a Viewer and a Tracker. Observe `VWR` badge on Tracker.
*   **Verification:** 
    1. **VWR Badge Consistency**: Confirm the VWR badge on the Tracker turns Green/Cyan when the Viewer is active. Red on Tracker while Green on Viewer is a failure (Asymmetry).
    2. **Geofence Sync**: Confirm the Tracker receives and logs "Received geofences from remote Viewer" when points are modified on the Viewer.

---
*This document is a specialized shard of TEST_PROCEDURE.md to ensure the integrity of the critical Operational Forensic Audit phase.*
