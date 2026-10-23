# SOT Master Requirements & Hardening Status (Oct23.108)

## 🏗️ Architectural Master Rules (330 Rules)

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
*   **1.247 Signaling Identity Symmetry (R-ID 616)**: All signaling events (pulses, pings, pongs, telemetry) MUST undergo strict identity validation against the target role's configured ID to prevent telemetry leaks and erroneous connectivity badges. (Oct22.12 - ISSUE-OCT22.0-F3).
*   **1.248 Telemetry Sync Parity (R-ID 617)**: The signaling protocol MUST ensure raw forensic sensor data (Lux, Acoustic, Vibration) and real-time RTT are synchronized across peers to ensure Viewer HUD accuracy. (Oct22.13 - ISSUE-OCT22.0-F4).
*   **1.249 Forensic State Symmetry (R-ID 618)**: The system MUST ensure session audit metrics (e.g., Violation Percentage) and adaptive sensor thresholds (e.g., Vibration Floor) are synchronized across peers to maintain forensic parity between Tracker and Viewer evaluation engines. (Oct22.16 - ISSUE-OCT22.0-F4).
*   **1.250 Jammer Discrimination Parity (R-ID 619)**: The system MUST synchronize the `isJammingCandidate` flag across all signaling (Protobuf/JSON) and persistence layers to ensure the Viewer HUD correctly reflects the Tracker's jammer discrimination state. (Oct22.16 - ISSUE-OCT22.0-F4).
*   **1.251 Forensic State Raw Sensor Parity (R-ID 620)**: The system MUST ensure raw sensor snapshots (Lux, Acoustic) are preserved in forensic traces and synchronized across all signaling and persistence layers to enable high-fidelity post-mortem analysis. (Oct23.00 - ISSUE-OCT22.16-F1).
*   **1.252 Discovery Mode Asymmetry (R-ID 621)**: The signaling validator MUST permit Trackers in a default state (no Viewer ID assigned) to accept incoming pulses from any viewer, enabling initial discovery and pairing (ISSUE-OCT24.00-F2).
*   **1.253 Broadcast Export Security (R-ID 622)**: All internal broadcast receivers MUST be registered with `RECEIVER_NOT_EXPORTED` unless explicit cross-app communication is required, satisfying Android 14+ security mandates (ISSUE-OCT24.00-F3).
*   **1.254 BackHandler Composition Local (R-ID 623)**: Back button interception MUST be consolidated into a reusable `LocalBackAction` Composition Local to reduce boilerplate and ensure consistent navigation behavior across all overlays (Oct23.100 - SIMP-OCT22.10-S1).
*   **1.255 Telemetry State Preservation (R-ID 624)**: Telemetry mapping logic MUST utilize the current cached state as the fallback for optional fields during JSON/Protobuf deserialization to prevent state regression (flicker) when keys are missing. (Oct23.106 - ISSUE-OCT23.101-F2).
*   **1.256 Delta Drift Prevention (R-ID 625)**: `TelemetryProtobufMapper` MUST perform a periodic absolute coordinate refresh (every 50 packets) to prevent permanent coordinate drift in delta-compressed signaling streams. (Oct23.107 - ISSUE-OCT23.102-F7).
*   **1.257 Protobuf State Protection (R-ID 626)**: Telemetry mapping MUST utilize hasField() checks for all optional Protobuf scalar fields to prevent default value pollution during deserialization. (Oct23.108 - ISSUE-OCT23.102-F8).

## 🛡️ Core Hardening Baseline
*   **SOT ID 820**: Protobuf Default Value Pollution Remediation - PASSED. Applied `optional` keyword to scalar fields in `app_settings.proto` and hardened `TelemetryMapper.kt` with `hasField()` checks. (Oct23.108 - ISSUE-OCT23.102-F8).
*   **SOT ID 819**: Delta Compression Drift Prevention - PASSED. Implemented periodic absolute coordinate refresh in `TelemetryProtobufMapper` (R-ID 625). (Oct23.107 - ISSUE-OCT23.102-F7).
*   **SOT ID 818**: Telemetry Mapping State Preservation - PASSED. Hardened `TelemetryMapper.kt` to preserve cached values (prox, rtt, snapshots) during JSON mapping. (Oct23.106 - ISSUE-OCT23.101-F2).
*   **SOT ID 817**: BackHandler Consolidation - PASSED. Implemented `LocalBackAction` and `BackPressInterceptor` to unify back navigation across all overlays. (Oct23.100 - SIMP-OCT22.10-S1).
*   **SOT ID 816**: Build Stabilization & Lint Hardening - PASSED. Resolved dependency conflicts, upgraded to JVM 17, and applied mandatory broadcast receiver export flags. (Oct24.00 - ISSUE-OCT24.00-F1/F3).
*   **SOT ID 815**: Discovery Mode & Pairing Logic - PASSED. Relaxed SignalingValidator discovery mode to allow Trackers with default IDs to adopt custom viewers. (Oct24.00 - ISSUE-OCT24.00-F2).
*   **SOT ID 814**: Forensic Trace Raw Sensor Parity - PASSED. Extended ForensicSnapshot and LocationUpdate with lux and acoustic fields for full raw sensor trace parity. (Oct23.00 - ISSUE-OCT22.16-F1).
*   **SOT ID 813**: Telemetry Propagation & Violation Relay - PARTIALLY REMEDIATED (Oct23.106). Resolved merge gaps in `TelemetryMerger` (F1) and mapping gaps in `TelemetryMapper` (F2). Residual gaps remain in persistence (F4). (ISSUE-OCT22.0-F4).
*   **SOT ID 812**: Signaling Identity Symmetry & Leak Prevention - PASSED. Enforced strict ID symmetry in SignalingValidator and blocked cross-viewer traffic. (Oct22.12 - ISSUE-OCT22.0-F3).
*   **SOT ID 811**: Setup Exit Authority & Lifecycle Hardening - PASSED. Implemented BackHandler and Close icons for all setup overlays. (Oct22.10 - ISSUE-OCT22.0-F2).
*   **SOT ID 810**: Setup & Configuration Hardening - PASSED. Resolved ID persistence blocks, added Load Defaults, and implemented explicit Exit authority. (Oct22.0 - ISSUE-OCT22.0-F1).
*   **SOT ID 801**: Asymmetric Connectivity & Geofence Sync Resolution - PASSED. (Oct21.06).

## ✅ Verification Chapters
*   **Chapter 4.2 (Signaling Protocol Hardening)**: PASSED - Remediation of Protobuf default pollution (Oct23.108) and delta drift prevention (Oct23.107).
*   **Chapter 4.0 (Telemetry Continuity Audit)**: PARTIALLY REMEDIATED - Resolved logic gaps in TelemetryMerger (Oct23.105) and mapping in TelemetryMapper (Oct23.106). (Oct23.106).
*   **Chapter 4.1 (Build & Infrastructure Audit)**: PASSED - Verified JVM 17 alignment, Hilt Test compatibility, and Broadcast Export security. (Oct24.00).
*   **Chapter 3.0 (Signaling Identity Audit)**: PASSED - Enforced strict symmetry in SignalingValidator; verified pulse/ping/pong isolation. (Oct22.12).
*   **Chapter 2.9 (Overlay Lifecycle Audit)**: PASSED - Verified BackHandler interception and Exit authority in Phone Setup. Migrated to Composition Local in Oct23.100. (Oct23.100).
*   **Chapter 2.8 (Setup Persistence Audit)**: PASSED - Verified ID saving, Relay URL visibility, and Load Defaults behavior. (Oct22.0).
