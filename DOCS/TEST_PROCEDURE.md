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
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **1.3 Permission Onboarding:** 
    *   Launch the app and grant all requested permissions.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.
*   **1.4 Landing Page Stability:** 
    *   Stay on the landing page for 15 minutes.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟢 **Physical: PASSED**.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button.
*   **2.2 Exercise Setup Options:**
    *   Verify System Diagnostics (Red/Green state update).
*   **2.3 Sensor Calibration:**
    *   Adjust sensitivity sliders for Vibration/Tilt.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED**.

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

## Chapter 4 - Viewer Mode & Remote Sync
**Goal:** Validate real-time synchronization.

*   **4.1 Viewer Setup:** ID sync.
*   **4.2 Remote HUD Sync:**
    *   Trigger alarm on Tracker; verify on Viewer.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟡 **Physical: VERIFYING**.
*   **4.3 Temporal Authority Check:**
    *   Verify `isGpsFresh` uses receipt-time deltas.

## Chapter 5 - Recovery and Edge Cases
**Goal:** Verify system resilience against signal loss.

*   **5.1 GNSS Zombie Recovery (Issue #905 / R252):**
    *   Place device in shielded area; move to clear view.
    *   **Verification:** Observe if `SIGNAL LOSS` clears on HUD within 30s.
    *   **Status (Sep.05.06):** 🟢 **Logical: PASSED** | 🟡 **Physical: VERIFYING**.

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
*   **9:** Audio/Siren Latency.
*   **10:** Native Resource Lifecycle (BaseEventQueue disposal).
*   **11:** Geofence Uncertainty Drift (R460).
*   **12:** Database Adaptive Pruning (Low Storage).
*   **13:** Remote Command Execution (Siren/Mode).
*   **14:** Forensic Ribbon Persistence.
*   **15:** Power Disconnect Audit (±10ms).
*   **16:** Network Resilience & Relay Failover.
*   **17:** Forensic Log Export Integrity.
*   **18:** Physical Tamper Escalation (Light/Acoustic).
*   **19:** Multi-Viewer Sync Consistency.
*   **20:** Schema Migration Trace Preservation.

## Chapters 21-100: Advanced Forensic & System Chapters
*   **21-30:** Connectivity Hardening (WebSocket backoff, fragmentation, signature collision, RTL HUD).
*   **31-40:** Logic & System Resilience (Predictive exit, urban drift mitigation, Jump Engine Tier 3, Barometric precision).
*   **41-50:** Storage & Hardware (WAL performance, Spill-buffer wrap-around, Magnetometer figure-8, Hilt death).
*   **51-60:** Security & Performance (URL Injection validation, log encryption overhead, 24h Soak Test).
*   **61-70:** Advanced Forensics (Multi-sensor correlation, SNR Snapshots, Spill-buffer replay latency).
*   **71-80:** System Integration (Spatial quantization, WorkManager expedited recovery, Acoustic floor stability).
*   **81-90:** Reliability Benchmarks (SNR Penalty distribution, App Standby Bucket policy, Mali Driver recovery).
*   **91-100:** Final SOT Compliance (Shadow-cache eviction, Initial frame stall mitigation, 48h Continuity Audit).

---
**Total Testing Chapters: 100**
*(Full historical procedure restored Sep.05.23)*
