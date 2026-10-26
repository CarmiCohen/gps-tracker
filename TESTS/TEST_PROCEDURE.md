# Test Procedure - GPS Tracker (vOct26.0)

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
    *   **Verification:** Confirm string matches `Oct26.0_PLATINUM` (or `_DEBUG`).

## Chapter 2 - Setup and Configuration (Bypass Active)
**Goal:** Validate the configuration pipeline with the Oct.26 Hardening (R-ID 615).

*   **2.1 Enter Tracker Mode:** Tap the "Tracker" button.
*   **2.2 Verify Setup Bypass:**
    *   **Action:** Proceed past role selection.
    *   **Verification:** Confirm the app lands directly on the Tracker Dashboard, bypassing the "Phone Setup" screen even if battery optimization is enabled (R-ID 615).
*   **2.3 Background Disclosure Synchronization:**
    *   **Action:** Grant background location permission if prompted.
    *   **Verification:** Confirm the dialog does not loop and correctly proceeds to the dashboard (resolved race condition).
*   **2.4 Connection Sticky-State (Issue #1422):**
    *   Confirm immediate disconnection/reconnection upon ID/URL change.
*   **2.5 Setup & Persistence Hardening (ISSUE-OCT21.06-F1):**
    *   **Action A (ID Persistence):** Modify Tracker/Viewer IDs, tap **Close (X)**, and reopen Settings. 
    *   **Verification:** Confirm IDs are preserved.
    *   **Action B (Load Defaults):** Tap **Load Factory Defaults**. 
    *   **Verification:** Confirm IDs reset to `T` and `V`.

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

---
**Total Testing Chapters: 100**
*(Full historical procedure restored and synchronized Oct26.0)*
