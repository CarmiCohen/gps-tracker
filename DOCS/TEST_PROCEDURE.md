# Test Procedure - GPS Tracker (vSep.05.06)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

### 🚩 Result Flags Legend:
*   **Logical Status**: Verifies that the code implementation follows the architectural requirements and emits correct log signatures (e.g., pulses triggering in Logcat).
*   **Physical Status**: Verifies that the hardware (A15/S21FE) and environment (Relay) physically resolve the failure state (e.g., SRV turns Green, GPS fix is regained).

---

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app from the test device to clear shared preferences and local databases.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**. Deployed vSep.05.06 to A15.
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page for 15 minutes.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **2.2 Exercise Setup Options:**
    *   Verify System Diagnostics (Red/Green state update).
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy and sentinel logic.

*   **3.1 Main Screen Completeness:**
    *   Verify HUD elements and Stationary status.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **3.2 Physical Sentinel (Alarm Logic):**
    *   Vibration Test.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

## Chapter 4 - Viewer Mode & Remote Sync
**Goal:** Validate real-time synchronization.

*   **4.2 Remote HUD Sync:**
    *   Trigger alarm on Tracker; verify on Viewer.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟡 **Physical: VERIFYING**. Protocol aligned with Socket.io v4. Handshake verification pending.

## Chapter 5 - Recovery and Edge Cases
**Goal:** Verify system resilience against signal loss.

*   **5.1 GNSS Zombie Recovery (Issue #905 / R252):**
    *   Place device in shielded area; move to clear view.
    *   **Verification:** Observe if `SIGNAL LOSS` clears on HUD within 30s.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟡 **Physical: VERIFYING**. Raw Provider Bypass implemented in HardwareProvider.kt.

## Chapter 16 - Network Resilience & Relay Failover
**Goal:** Verify connection stability.

*   **16.1 Signaling Transport Robustness (Issue #912):**
    *   **Verification:** Ensure `SRV` turns Green via WebSocket.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟡 **Physical: VERIFYING**. Client locked to v2.1.2 with strict WebSocket transport.

---
## Test Log: Sep.05.06
*   **Chapter 5.1:** 🟢 Logical: PASSED. Raw Provider Bypass and Hardware Looper Fix (#893) implemented. Chipset reset command verified in logs.
*   **Chapter 16.1:** 🟢 Logical: PASSED. Direct websocket logic finalized; aligned with Node v4 protocol.
*   **Status (Sep.05.06):** Hardening package fully implemented. Physical verification on A15 is the only remaining blocker.
