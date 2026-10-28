# Operational Forensic Audit - GPS Tracker (vOct28.23)

**Goal:** Verify system health via runtime telemetry and log signatures during deployment. This chapter contains high-frequency audit points that must be verified for every production-ready build.

---

## 0.1 Logcat Anomaly Monitoring
*   **Action:** Deploy the app and filter Logcat for `StackLog`, `Davey`, or `Exception`.
*   **Verification:** 
    1. **Steady-State Enforcement**: Confirm zero `StackLog` leaks during steady-state operation. 
    2. **Startup Performance**: Confirm zero `Davey` events > 500ms and zero `StackLog` signatures during the first 10 seconds of startup. Any detection of these signatures constitutes an audit failure on budget hardware (A15).
    3. Monitor for `Davey` events > 500ms during active 50Hz sensor bursts post-startup.

## 0.2 Event Log Continuity
*   **Action:** Observe "Stay-alive pulse" and "PRUNING" signatures in Logcat.
*   **Verification:** Confirm pulses trigger every 30s. Confirm maintenance tasks execute without blocking the main thread.

## 0.3 Tracker Stealth & Authority Audit (Issue #Oct20.31-F1)
*   **Action:** Deploy the app in Tracker Mode and inspect the UI, HUD, and Settings.
*   **Verification:** **STEALTH BALANCE (Updated Oct21.06)**. 
    1. Confirm HUD contains NO **ALM** badge or **ALARM** indicators.
    2. Confirm HUD contains NO **LOCK** or interactive alarm control buttons.
    3. Confirm HUD contains NO **SIREN** indicators or audio activity.
    4. **Settings Gear (RESTORED)**: Confirm settings gear is visible to allow geofence (fence) state verification (R797).
    5. Confirm Map contains NO **Home Point** management tools (ADD/DEL).
    6. Tracker must remain a pure telemetry provider; all logic/alarm authority resides exclusively on the Viewer.

## 0.4 Back Button Lifecycle (Issue #1424)
*   **Action:** Tap the system "Return/Back" button while on the Viewer or Tracker screen.
*   **Verification:** Confirm the app DOES NOT close or cease operations. It should either stay on the current screen (if it's the root) or navigate back correctly without process termination.

## 0.5 UI Alignment & Occlusion Audit (Issue #1425, #1426, #OCT20.33-F2)
*   **Action:** Inspect the Viewer UI in various orientations.
*   **Verification:** 
    1. **Version Position**: Confirm the version string (e.g., `Oct28.23`) is at the absolute bottom-start edge and does NOT overlap with the Map Scale bar (R-ID 603).
    2. **HUD Visibility**: Confirm the upper HUD header has a Slate900 scrim.
    3. **HUD Compression**: Verify the telemetry HUD is NOT squeezed and buttons have proportional padding.
    4. **Debug Suppression**: Confirm `UI_READY` label is suppressed in PLATINUM builds (R-ID 610).

## 0.6 Peer Sync & Connectivity Audit (Issue #ISSUE-OCT21.05-F2)
*   **Action:** Connect a Viewer and a Tracker. 
*   **Verification:** 
    1. **VWR Badge Consistency (R-ID 605)**: Confirm the **VWR** badge on the Tracker turns Green/Cyan within 10s of the Viewer starting. Red on Tracker while Green on Viewer is a failure (Asymmetry).
    2. **Proactive Geofence Sync (R-ID 606)**: Upon initial connection (without manual mutation), verify the Tracker receives and logs "SYNC: Received geofences from remote Viewer."
    3. **Manual Geofence Sync**: Confirm that adding/removing a point on the Viewer propagates to the Tracker map within 5 seconds.

## 0.7 Setup Closure & Exit Authority (Issue #ISSUE-OCT22.0-F2)
*   **Action:** Open Setup. Attempt to close via UI button (X). If missing or failing, use system Back button.
*   **Verification:** 
    1. UI Button (X) must exist and successfully close the overlay.
    2. System Back button must close the overlay ONLY and NOT cause the entire application to exit.

## 0.8 ID Isolation & Asymmetry Audit (Issue #ISSUE-OCT22.0-F3)
*   **Action:** Configure mismatched IDs (e.g., Viewer: T2/V2 vs Tracker: T/V).
*   **Verification:** 
    1. **Strict Isolation**: Viewer TRK badge must remain RED. Tracker VWR badge must remain RED.
    2. **Telemetry Suppression**: The Info screen must NOT show active/green values when no authorized connection exists.

## 0.9 Factory Default & Telemetry Continuity Audit (Issue #ISSUE-OCT22.0-F4)
*   **Action:** Load Factory Defaults on both units. Observe telemetry flow.
*   **Verification:**
    1. **Ping Integrity**: Ping values must be visible on both devices' info pages once connected.
    2. **Sensor Parity**: Telemetry values (Noise dB, Lux lx) must match between Tracker (Source) and Viewer (Remote).
    3. **Violation Propagation**: If Tracker displays an "Acoustic Violation," the Viewer HUD/Info must mirror this state immediately.

---
*This document is a specialized shard of TEST_PROCEDURE.md to ensure the integrity of the critical Operational Forensic Audit phase.*
