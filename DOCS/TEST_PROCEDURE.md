# Test Procedure - GPS Tracker (vSep.29.0)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

### 🚩 Result Flags Legend:
*   **Logical Status**: Verifies that the code implementation follows the architectural requirements and emits correct log signatures (e.g., pulses triggering in Logcat).
*   **Physical Status**: Verifies that the hardware (A15/S21FE) and environment (Relay) physically resolve the failure state (e.g., SRV turns Green, GPS fix is regained).

---

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions.
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page for 15 minutes.
*   **1.5 Role Selection UX Branding (Issue #1177):**
    *   **Action:** Launch app with no remote peer active.
    *   **Verification:** Confirm "VIEWER MODE" card is dimmed (`Slate500`). Activate a remote tracker and verify card color transitions to `ViewerCyan` automatically.
    *   **Status (Sep.22.00):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button.
*   **2.2 Exercise Setup Options:**
    *   Verify System Diagnostics (Red/Green state update).
*   **2.3 Sensor Calibration:**
    *   Adjust sensitivity sliders for Vibration/Tilt.
*   **2.4 Geofence & Home Point Management (Issue #1179):**
    *   **Action:** Enter geofence addition/removal mode and execute high-frequency rapid updates/taps to add and remove multiple home points.
    *   **Verification:** Verify that `geofenceMode` persists (does not reset to `IDLE`) during batch operations, enabling friction-less batch updates. Confirm `isFenceVisible` is forced to `true` upon entry for immediate visual feedback. Ensure atomic DataStore mutations prevent any list mutation corruption or race conditions.
    *   **Status (Sep.22.03):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **2.5 DataStore Mutation Extension (Issue #1180):**
    *   **Action:** Invoke consecutive save operations for configuration values, metrics, and home points.
    *   **Verification:** Confirm all updates compile correctly under the unified `mutate` extension and resolve sequentially without data race visibility or state inconsistency under high load.
    *   **Status (Sep.22.04):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **2.6 UseCase Functional Consolidation (Issue #1181):**
    *   **Action:** Invoke map overlays, geofence mutations, and spatial events simultaneously under rapid screen interactions.
    *   **Verification:** Verify that all calls flow flawlessly into `SpatialLogicUseCase` without any race conditions, data inconsistencies, or behavioral regression compared to old standalone models.
    *   **Status (Sep.22.05):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy and sentinel logic.

*   **3.1 Main Screen Completeness:**
    *   Verify HUD elements and Stationary status.
*   **3.2 Physical Sentinel (Alarm Logic):**
    *   Vibration Test.
    *   Tilt Test.
    *   Light-Jump.
*   **3.3 Service Persistence:**
    *   Swipe app away; verify foreground notification persists.
*   **3.4 Status Row Presentation (Issue #1176, #1178):**
    *   **1176 SI Unit Suffix:** Verify temperature displays as `X°` (e.g., `35°`).
    *   **1178 GNSS Initialization:** Verify satellite counts show `--/--` before hardware initialization, then transition to numeric values.
    *   **Status (Sep.22.00):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **3.5 Tracker Map Stability (Issue #1383):**
    *   **Action:** Tap zoom buttons and perform pinch gestures on the map.
    *   **Verification:** Verify map does not enter "Autonomous Zoom-In" loop. Confirm zoom-out functions correctly.
*   **3.6 HUD Velocity Logic (Issue #1386):**
    *   **Action:** Observe HUD state while stationary.
    *   **Verification:** Verify "MOVING" status is not displayed when speed is 0.0 km/h.

## Chapter 4 - Viewer Mode & Remote Sync
**Goal:** Validate real-time synchronization.

*   **4.1 Viewer Setup:** ID sync.
*   **4.2 Remote HUD Sync:**
    *   Trigger alarm on Tracker; verify on Viewer.
*   **4.3 Temporal Authority Check:**
    *   Verify `isGpsFresh` uses receipt-time deltas.
*   **4.4 Spontaneous Alarm Audit (Issue #1382):**
    *   **Action:** Deploy app and monitor Viewer behavior without triggering alarms.
    *   **Verification:** Confirm no siren sounds immediately after deployment or autonomously thereafter. Verify that Global Mute and Volume controls correctly suppress audio.
*   **4.5 Ribbon UX Legibility (Issue #1384):**
    *   **Action:** Inspect the Ribbon time ruler.
    *   **Verification:** Verify time ruler text and markings are clearly legible and not occluded or blurred.
*   **4.6 Peer Connectivity (Issue #1385):**
    *   **Action:** Verify Diagnostic LEDs on both devices.
    *   **Verification:** Confirm GPS, TRK, DAT (Viewer) and VWR (Tracker) transition to Green/Cyan indicating active link.

## Chapter 5 - Recovery and Edge Cases
**Goal:** Verify system resilience against signal loss.

*   **5.1 GNSS Zombie Recovery (Issue #905 / R252):**
    *   Place device in shielded area; move to clear view.
    *   **Verification:** Observe if `SIGNAL LOSS` clears on HUD within 30s.

## Chapter 6 - Forensic Stress Testing
*   **6.1 Manual Forensic Stress Test:** Trigger via Diagnostics.
*   **6.2 Heat Mitigation Validation:** Simulate thermal limit (COOLING MODE).

## Chapter 7 - Architectural Integrity & Performance
*   **7.1 UI Performance Audit:** Check for Davey events during 100Hz bursts.
*   **7.2 DI/Hilt Stability:** Cold-start service after process death.

## Chapter 8 - Validation Hooks & Aggregation
*   **8.1 Forensic Stall Simulation:** EMA reliability drop check.
*   **8.2 State Aggregation Stability:** Rapid HUD transitions.

## Chapters 9-20: Hardening Baselines
*   **Status (Sep.22.05):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

## Chapters 21-100: Advanced Forensic & System Chapters
*   **Status (Sep.22.05):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

---
**Total Testing Chapters: 100**
*(Full historical procedure synchronized Sep.29.0)*
