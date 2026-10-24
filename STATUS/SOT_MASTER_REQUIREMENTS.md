# SOT Master Requirements & Hardening Status (Oct24.2)

## 🏗️ Architectural Master Rules (334 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0).
*   **(Rules 1.5 - 1.241 archived or listed in previous versions).**
*   **1.242 Setup Authority (R-ID 611)**: The Settings UI provide an explicit mechanism and a factory reset authority to prevent setup entrapment.
*   **1.243 Configuration Persistence (R-ID 612)**: Mutations MUST be captured in a persistent draft state and atomically committed to DataStore.
*   **1.244 Identity Uniqueness (R-ID 613)**: The system MUST prevent commitment of non-unique or reserved IDs.
*   **1.245 Overlay Lifecycle Guard (R-ID 614)**: Critical setup overlays MUST implement `BackHandler` to intercept system navigation.
*   **1.246 Setup Exit Parity (R-ID 615)**: The Phone Setup overlay MUST provide an explicit Close (X) mechanism.
*   **1.247 Signaling Identity Symmetry (R-ID 616)**: All signaling events MUST undergo strict identity validation against the target role's configured ID.
*   **1.248 Telemetry Sync Parity (R-ID 617)**: The signaling protocol MUST ensure raw forensic sensor data and real-time RTT are synchronized across peers.
*   **1.249 Forensic State Symmetry (R-ID 618)**: The system MUST ensure session audit metrics and adaptive sensor thresholds are synchronized across peers.
*   **1.250 Jammer Discrimination Parity (R-ID 619)**: The system MUST synchronize the `isJammingCandidate` flag across all signaling and persistence layers.
*   **1.251 Forensic State Raw Sensor Parity (R-ID 620)**: The system MUST ensure raw sensor snapshots (Lux, Acoustic) are preserved in forensic traces and synchronized.
*   **1.252 Discovery Mode Asymmetry (R-ID 621)**: The signaling validator MUST permit Trackers in a default state to accept incoming pulses from any viewer.
*   **1.253 Broadcast Export Security (R-ID 622)**: All internal broadcast receivers MUST be registered with `RECEIVER_NOT_EXPORTED` unless explicit cross-app communication is required.
*   **1.254 BackHandler Composition Local (R-ID 623)**: Back button interception MUST be consolidated into a reusable `LocalBackAction` Composition Local.
*   **1.255 Telemetry State Preservation (R-ID 624)**: Telemetry mapping logic MUST utilize the current cached state as the fallback for optional fields during deserialization.
*   **1.256 Delta Drift Prevention (R-ID 625)**: `TelemetryProtobufMapper` MUST perform a periodic absolute coordinate refresh (every 50 packets).
*   **1.257 Protobuf State Protection (R-ID 626)**: Telemetry mapping MUST utilize hasField() checks for all optional Protobuf scalar fields.
*   **1.258 Concurrency-Safe Telemetry Processing (R-ID 627)**: `ConnectivitySuite` MUST NOT utilize shared member flyweights for telemetry processing.
*   **1.259 Hysteresis State Parity (R-ID 628)**: The system MUST synchronize `stationaryDurationMs` across all signaling and persistence layers.
*   **1.260 Violation Integrity Propagation (R-ID 629)**: Violation events MUST propagate the `isJammingCandidate` flag through all reporting and persistence layers.
*   **1.261 Offline Violation Synchronization (R-ID 630)**: Discrete violation events (`violation_event`) MUST be buffered in persistent storage when the device is offline and synchronized automatically upon network recovery. (Oct24.2 - ISSUE-OCT23.101-F6).

## 🛡️ Core Hardening Baseline
*   **SOT ID 824**: Offline Violation Event Sync Remediation - PASSED. Implemented persistent buffering for `violation_event` signaling in `OfflineRepository` and `ConnectivitySuite` (R-ID 630). (Oct24.2 - ISSUE-OCT23.101-F6).
*   **SOT ID 823**: Violation Persistence Integrity Remediation - PASSED. Synchronized `isJammingCandidate` across violation reporting and persistence layers (R-ID 629). (Oct23.121 - ISSUE-OCT23.101-F4).
*   **SOT ID 822**: Stationary Duration Propagation Remediation - PASSED. Synchronized `stationaryDurationMs` across Protobuf, JSON, and Room persistence layers (R-ID 628). (Oct23.120 - ISSUE-OCT23.101-F3).
*   **SOT ID 821**: Telemetry Flyweight Race Condition Remediation - PASSED. Replaced shared flyweights in `ConnectivitySuite.kt` with local instances and atomic references to ensure concurrency safety. (Oct23.109 - ISSUE-OCT23.102-F9).
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
*   **Chapter 4.3 (Offline Synchronization Audit)**: PASSED - Verified persistent buffering and delivery of violation events after network recovery. (Oct24.2 - ISSUE-OCT23.101-F6).
*   **Chapter 4.2 (Signaling Protocol Hardening)**: PASSED - Remediation of telemetry flyweight race condition (Oct23.109), Protobuf default pollution (Oct23.108) and delta drift prevention (Oct23.107).
*   **Chapter 4.0 (Telemetry Continuity Audit)**: PASSED - Resolved logic gaps in TelemetryMerger (Oct23.105), mapping in TelemetryMapper (Oct23.106), stationary duration propagation (Oct23.120), and violation integrity (Oct23.121). (Oct23.121).
*   **Chapter 4.1 (Build & Infrastructure Audit)**: PASSED - Verified JVM 17 alignment, Hilt Test compatibility, and Broadcast Export security. (Oct24.00).
*   **Chapter 3.0 (Signaling Identity Audit)**: PASSED - Enforced strict symmetry in SignalingValidator; verified pulse/ping/pong isolation. (Oct22.12).
*   **Chapter 2.9 (Overlay Lifecycle Audit)**: PASSED - Verified BackHandler interception and Exit authority in Phone Setup. Migrated to Composition Local in Oct23.100. (Oct23.100).
*   **Chapter 2.8 (Setup Persistence Audit)**: PASSED - Verified ID saving, Relay URL visibility, and Load Defaults behavior. (Oct22.0).
