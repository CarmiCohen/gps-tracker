# SOT Master Requirements & Hardening Status (Oct22.10)

## 🏗️ Architectural Master Rules (319 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0).
*   **(Rules 1.5 - 1.241 archived or listed in previous versions).**
*   **1.242 Setup Authority (R-ID 611)**: The Settings UI MUST provide an explicit, non-destructive exit mechanism (Close) and a factory reset authority (Load Defaults) to prevent setup entrapment. (Oct22.0 - ISSUE-OCT22.0-F1).
*   **1.243 Configuration Persistence (R-ID 612)**: All configuration mutations (IDs, URL, Geofence) MUST be captured in a persistent draft state and atomically committed to DataStore to prevent data loss on process death. (Oct22.0 - ISSUE-OCT22.0-F1).
*   **1.244 Identity Uniqueness (R-ID 613)**: The system MUST prevent commitment of non-unique or reserved IDs (e.g., T/V vs Trk/viewer) to prevent relay cross-talk and validation errors. (Oct22.0 - ISSUE-OCT22.0-F1).
*   **1.245 Overlay Lifecycle Guard (R-ID 614)**: Critical setup overlays MUST implement `BackHandler` to intercept system navigation and prevent unintended application termination during configuration. (Oct22.10 - ISSUE-OCT22.0-F2).
*   **1.246 Setup Exit Parity (R-ID 615)**: The Phone Setup overlay MUST provide an explicit Close (X) mechanism identical to the Settings overlay to ensure consistent exit authority. (Oct22.10 - ISSUE-OCT22.0-F2).

## 🛡️ Core Hardening Baseline
*   **SOT ID 811**: Setup Exit Authority & Lifecycle Hardening - PASSED. Implemented BackHandler and Close icons for all setup overlays. (Oct22.10 - ISSUE-OCT22.0-F2).
*   **SOT ID 810**: Setup & Configuration Hardening - PASSED. Resolved ID persistence blocks, added Load Defaults, and implemented explicit Exit authority. (Oct22.0 - ISSUE-OCT22.0-F1).
*   **SOT ID 801**: Asymmetric Connectivity & Geofence Sync Resolution - PASSED. (Oct21.06).
*   **SOT ID 800**: Unit Test Version Mismatch Resolution - PASSED. (Oct21.05).
*   **SOT ID 799**: UI Overlap Resolution - PASSED. (Oct21.04).
*   **SOT ID 798**: Debug Artifact Suppression - PASSED. (Oct21.03).
*   **SOT ID 797**: Settings Gear Restoration - PASSED. (Oct21.02).
*   **SOT ID 796**: Protocol Routing Integrity - PASSED. (Oct21.01).
*   **SOT ID 795**: HUD Vertical Spacing / Touch Targets - PASSED. (Oct21.00).

## ✅ Verification Chapters
*   **Chapter 2.9 (Overlay Lifecycle Audit)**: PASSED - Verified BackHandler interception and Exit authority in Phone Setup. (Oct22.10).
*   **Chapter 2.8 (Setup Persistence Audit)**: PASSED - Verified ID saving, Relay URL visibility, and Load Defaults behavior. (Oct22.0).
*   **Chapter 0.6 (Peer Sync Audit)**: PASSED - (Oct21.06).
*   **Chapter 1.3 (UI Hardening Audit)**: PASSED - (Oct21.04).
*   **Chapter 1.2 (Debug Artifacts Audit)**: PASSED - (Oct21.03).
*   **Chapter 0.9 (Settings Audit)**: PASSED - (Oct21.02).
*   **Chapter 0.8 (Signaling Audit)**: PASSED - (Oct21.01).
*   **Chapter 0.7 (Layout Audit)**: PASSED - (Oct21.00).
