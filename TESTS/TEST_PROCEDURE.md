# Test Procedure - GPS Tracker (vNov2.1 - BASELINE STABILIZED)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

---

## Chapter 0 - Operational Forensic Audit
**Status:** 📂 Moved to separate file to ensure persistence and integrity.
**Link:** [OPERATIONAL_FORENSIC_AUDIT.md](OPERATIONAL_FORENSIC_AUDIT.md)

---

## Chapter 1 - Deployment & Initial Launch
*   **1.1 Environment Reset:** Uninstall existing versions.
*   **1.2 Deployment:** Deploy via Android Studio (Nov2.1).
*   **1.3 Version Verification:** Confirm string matches `Nov2.1_PLATINUM` (or `_DEBUG`).
*   **1.6 Branding Audit:** Verify the "Platinum" branding and JD logos are centered and high-res.

## Chapter 2 - Setup and Configuration (Bypass Active)
*   **2.1 Enter Tracker Mode:** Tap "Tracker".
*   **2.2 Verify Setup Bypass:** Confirm app lands directly on Dashboard (R-ID 615).
*   **2.3 ID Persistence:** Modify IDs, close (X), and reopen. Confirm preservation.

## Chapter 3 - Tracker Mode & Sensor Telemetry
*   **3.1 Main Screen Completeness:** Verify HUD elements and Stationary status.
*   **3.2 Physical Sentinel & Stealth Audit (R872):**
    *   **Action:** Shake device vigorously for 5s.
    *   **Verification:** ABSOLUTE STEALTH. Screen remains dark. Audio remains silent.
*   **3.3 Sensor JNI Bridge Verification:**
    *   **Action:** Cover the light sensor or tilt the device.
    *   **Verification:** Confirm `Lux` or `Tilt` values update in the HUD/Logs immediately.
*   **3.4 Service Persistence:** Swipe away; verify foreground notification persists.

## Chapter 4 - Viewer Mode & Remote Sync
*   **4.2 Remote HUD Sync:** Verify Alarm manifest ONLY appears on Viewer.
*   **4.3 State Sync:** Confirm Tracker and Viewer HUD transition between states simultaneously.

## Chapter 5 - GNSS & Persistence Gap (Physical)
*   **5.1 GNSS Zombie:** Shield in metal box (60s). Verify `SIGNAL LOSS` and recovery.
*   **5.2 Marker Persistence:** Kill app; confirm historical trail markers persist.

## Chapter 6 - Forensic Stress Testing
*   **6.1 Stress Latching:** Trigger "FORENSIC STRESS TEST" from Diagnostics. Verify `ACOUSTIC_VIOLATION` and `GPS_STALL` markers in log.

## Chapter 7 - Map & Geofencing (Expansion Phase)
*   **7.1 Stationary Anchor:**
    *   **Action:** Keep device still.
    *   **Verification:** Verify "ANCHOR LOCKED" badge appears after IMU confidence > 90%.
*   **7.2 Geofence Breakout:**
    *   **Action:** Move device > 20m from anchor.
    *   **Verification:** Confirm anchor release and immediate transition to "MOVING".
*   **7.3 Ghost Mode:**
    *   **Verification:** Markers/trails MUST dim (Slate500) if telemetry > 35s stale.

## Chapter 8 - Alarm & Siren Logic (Authority Audit)
*   **8.1 Siren Suppression (Tracker):** Confirm NO local audio or red-screen.
*   **8.2 Siren Authority (Viewer):** Confirm Viewer triggers full-screen Red Alert and 30s Siren.
*   **8.3 Mute Lockout:** Dismiss siren on Viewer. Confirm 30s manual mute lockout period.

## Chapter 12 - Maintenance & Pruning
*   **12.1 Database Pressure:** Trigger 100Hz Log Pressure. Verify `PruningWorker` reduces count.

## Chapter 14 - UI Performance & Ribbons
*   **14.1 Ribbon Persistence:** Toggle orientation. Confirm ribbons do not overlap.
*   **14.2 Frame Stability:** Observe Logcat for "Skipped frames" during sensor bursts.

## Chapter 15 - Power & Battery Tamper
*   **15.1 Power Tamper:** Unplug USB. Verify `POWER_TAMPER` event (±10ms).

## Chapter 16 - Network Resilience
*   **16.1 Relay Handoff:** Toggle Wi-Fi OFF. Verify `MOBILE` reconnection < 15s.

## Chapter 17 - Forensic Export
*   **17.1 Export Verification:** Verify `.json` contains hardware-IDs and scrubbed path metadata.

## Chapter 18 - System Integrity & Hardening
*   **18.1 Diagnostics Live-Refresh:**
    1. Revoke "Display over other apps" in system settings.
    2. Return to app Diagnostics and tap **REFRESH STATUS**.
    3. Verify "Overlay Permission" changes to **DENIED** (Red).
*   **18.2 Temporal Authority:**
    *   **Action:** Set Viewer system time -2 minutes relative to Tracker.
    *   **Verification:** HUD MUST remain Green. `isGpsFresh` must use receipt-time deltas, not absolute system time.
*   **18.3 DI/Hilt Cold-Start:**
    *   **Action:** Cold-start the app from a killed state (swipe away from recents).
    *   **Verification:** Ensure no `IllegalStateException` during `HistoryDao` or `LogDao` injection.

---
**Total Testing Chapters: 100**
*(Procedure synchronized with Nov2.1 Stabilization Baseline)*
