# SOT Master Requirements & Hardening Status (Oct20.32)

## 🏗️ Architectural Master Rules (310 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 - 1.231**: (Historical rules archived in `STATUS/sot_master_archive.md`).
*   **1.232 Back-Press Non-Termination (R-ID 601)**: The application MUST NOT terminate upon a single back-press while in active tracking or viewing modes. (Oct20.32 - ISSUE-OCT20.32-F1).
*   **1.233 Forensic Service Persistence (R-ID 602)**: Forensic services MUST remain active in the foreground even if the UI is dismissed via the back button. (Oct20.32 - ISSUE-OCT20.32-F1).
*   **1.234 Version String Alignment (R-ID 603)**: The version string box MUST be positioned at the bottom-most available coordinate of the UI container. (Oct20.32 - ISSUE-OCT20.32-F2).
*   **1.235 HUD Button Legibility (R-ID 604)**: All header navigation buttons MUST be clearly visible and distinguishable from the map background. (Oct20.32 - ISSUE-OCT20.32-F3).
*   **1.236 Bidirectional Connection State (R-ID 605)**: The Tracker MUST transition the VWR badge to Green/Cyan upon receipt of a valid `viewer_pulse` or `pong_activity`. (Oct20.32 - ISSUE-OCT20.32-F4).
*   **1.237 Geofence Synchronization (R-ID 606)**: Home point updates on the Viewer MUST be propagated to the Tracker within 5 seconds under normal network conditions. (Oct20.32 - ISSUE-OCT20.32-F5).

## 🛡️ Core Hardening Baseline
*   **SOT ID 793**: Home Point Synchronization - PASSED. Implemented `settings_relay` and automatic sync in `ConnectivitySuite`. (Oct20.32 - ISSUE-OCT20.32-F5).
*   **SOT ID 792**: Asymmetrical Connection State - PASSED. Fixed pulse processing in `ConnectivitySuite` to ensure Tracker role updates peer state. (Oct20.32 - ISSUE-OCT20.32-F4).
*   **SOT ID 791**: HUD Header Scrim/Occlusion - PASSED. Added `Slate900` scrim to upper HUD in `OverlayManager.kt`. (Oct20.32 - ISSUE-OCT20.32-F3).
*   **SOT ID 790**: Version String Alignment - PASSED. Realigned version tag to `BottomStart` with minimal padding. (Oct20.32 - ISSUE-OCT20.32-F2).
*   **SOT ID 789**: Back Button Termination Fix - PASSED. Verified `BackHandler` usage and activity lifecycle persistence. (Oct20.32 - ISSUE-OCT20.32-F1).
*   **SOT ID 788**: Storage-Aware Adaptive Pruning - PASSED. (Oct20.27).
*   **SOT ID 787**: Test Suite Version Regression - PASSED. (Oct20.25).
*   **SOT ID 786**: Storage Pressure Audit Flakiness - PASSED. (Oct20.24).
*   **SOT ID 785**: Redundant Connectivity Callback Leaks - PASSED. (Oct20.24).
*   **SOT ID 784**: Test Suite Version Regression - PASSED. (Oct20.22).
*   **SOT ID 783**: Forensic Circular Overwrite - PASSED. (Oct20.21).
*   **SOT ID 782**: Storage Test Race Conditions - PASSED. (Oct20.10).
*   **SOT ID 781**: Forensic Document Archival - PASSED. (Oct20.7).
*   **SOT ID 1 - 780**: (Historical baseline entries preserved in `STATUS/docs_history_archive.md`).

---

## ✅ Verification Chapters
*   **Chapter 0.6 (Peer Sync Audit)**: PASSED - Verified VWR badge logic and Geofence synchronization. (Oct20.32).
*   **Chapter 0.5 (UI Occlusion Audit)**: PASSED - Verified HUD scrim contrast and version alignment. (Oct20.32).
*   **Chapter 0.4 (Back Button Lifecycle)**: PASSED - Verified non-termination on return press. (Oct20.32).
*   **Chapter 12.7 (Adaptive Pruning Audit)**: PASSED. (Oct20.27).
*   **Chapter 12.6 (Regression Audit)**: PASSED. (Oct20.25).
*   **Chapter 92.1 (Connectivity Audit)**: PASSED. (Oct20.24).
*   **Chapter 12.2 (Test Determinism)**: PASSED. (Oct20.24).
*   **Chapter 1-100 (Full Hardening Cycle)**: PASSED. (Oct18.8).
