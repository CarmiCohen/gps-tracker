# Test Procedure - GPS Tracker (vOct24.32)

This document outlines the end-to-end manual testing protocol for the GPS Tracker application, ensuring high-assurance logic and forensic continuity.

---

## Chapter 0 - Operational Forensic Audit
**Status:** 📂 Moved to separate file to ensure persistence and integrity.
**Link:** [OPERATIONAL_FORENSIC_AUDIT.md](OPERATIONAL_FORENSIC_AUDIT.md)

---

## Chapter 1 - Deployment & Initial Launch
**Goal:** Verify clean installation and landing page stability.

*   **1.1 Environment Reset:** 
    *   Uninstall any existing versions of the app.
*   **1.2 Deployment:** 
    *   Deploy the latest build via Android Studio.
*   **1.3 Version Verification:**
    *   **Action:** Observe the version string.
    *   **Verification:** Confirm string matches `Oct24.32_PLATINUM`.

## Chapter 2 - Setup and Configuration
**Goal:** Validate the configuration pipeline and diagnostic tools.

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button.
*   **2.2 Exercise Setup Options:**
    *   Verify System Diagnostics (Red/Green state update).
*   **2.3 Sensor Calibration:**
    *   Adjust sensitivity sliders for Vibration/Tilt.
*   **2.4 Geofence & Home Point Management (Issue #1179):**
    *   Verify friction-less batch updates and atomic DataStore mutations.
*   **2.5 DataStore Mutation Extension (Issue #1180):**
    *   Confirm all updates resolve sequentially.
*   **2.6 UseCase Functional Consolidation (Issue #1181):**
    *   Verify SpatialLogicUseCase handles simultaneous events.
*   **2.7 Connection Sticky-State (Issue #1422):**
    *   Confirm immediate disconnection/reconnection upon ID/URL change.
*   **2.8 Setup & Persistence Hardening (ISSUE-OCT21.06-F1):**
    *   **Action A (ID Persistence):** Modify Tracker/Viewer IDs, tap **Close (X)**, and reopen Settings. 
    *   **Verification:** Confirm IDs are preserved and SRV/VWR badges update based on new IDs.
    *   **Action B (Exit Authority):** Tap the **X** icon in the header. 
    *   **Verification:** Confirm the overlay closes instantly without requiring a mode switch.
    *   **Action C (Load Defaults):** Tap **Load Factory Defaults**. 
    *   **Verification:** Confirm IDs reset to `T` and `V`, and Relay URL resets to production onrender address.
    *   **Action D (Relay Visibility):** Scroll to the bottom of Settings.
    *   **Verification:** Confirm Relay URL text is clearly white and legible against Slate background.

## Chapter 3 - Tracker Mode Operation
**Goal:** Verify telemetry accuracy and sentinel logic.

*   **3.1 Main Screen Completeness:**
    *   Verify HUD elements and Stationary status.
*   **3.2 Physical Sentinel & Stealth Audit (Issue #1391):**
    *   **ABSOLUTE STEALTH (R872)**. Verify Tracker remains dark/silent.
*   **3.3 Service Persistence:**
    *   Swipe app away; verify foreground notification persists.

## Chapter 4 - Viewer Mode & Remote Sync
**Goal:** Validate real-time synchronization.

*   **4.1 Viewer Setup:** ID sync.
*   **4.2 Remote HUD Sync & Alarm Authority (Issue #1391):**
    *   Verify Alarm manifest ONLY on Viewer.
*   **(Historical chapters 4.3 - 100 preserved in vOct20.33 reference).**

---
**Total Testing Chapters: 100**
*(Full historical procedure restored and synchronized Oct24.32)*
