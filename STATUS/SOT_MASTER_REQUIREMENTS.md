# System Source of Truth (SoT) - Aug.11.08 (Monitoring & Hardening)

This document serves as the definitive operational specification. All Issue IDs are Authoritative.

### 1. Performance & Startup Authority
*   **Forensic Integrity Verification (R143)**: (Added Aug.11.08) The system MUST verify that the Forensic Stress Test (R140) correctly triggers "Silent Failure" recordings (R133) when the device enters thermal throttling (Cooling Mode). The correlation engine MUST treat OS-level thermal safety measures as a valid stress indicator for GPS stalls. (Issue #143). **Status: Implemented & Verified.**
*   **Phone Setup Overlay Stabilization (R142)**: (Added Aug.11.06) The system MUST ensure that the `PhoneSetupOverlay` remains non-blocking and stable during entry and hydration on budget hardware (Samsung A15 class). (Issue #142). **Status: Implemented (Staggered Incremental Hydration).**
*   **Automated Forensic Stress Testing (R140)**: (Added Aug.11.05) The system MUST provide an internal mechanism to artificially saturate device resources (CPU > 85%, I/O > 40%) for a minimum of 5 seconds. (Issue #140). **Status: Implemented.**
*   **Forensic Anomaly Correlation Engine (R133)**: (Updated Aug.11.08) The system MUST implement cross-domain correlation between location stability and hardware resource stress. A `SILENT_FAILURE` MUST be flagged if a GPS stall is detected while `cpuLoad` >= 85%, `ioWait` >= 40%, `maxIoLatency` >= 800ms, OR `isThermalThrottling` is true, provided no physical tamper events are active (Issue #133-Sentinel). **Status: Implemented & Verified.**
*   **Compose Preview Coverage Authority (R136)**: (Added Aug.11.04) The system MUST maintain functional Compose Previews for all decomposed overlays (Issue #136). **Status: Implemented.**
*   **TrackerScreen ANR Remediation (R139)**: (Added Aug.11.02) The system MUST eliminate the 3000ms+ Main-thread stall occurring during the transition from LandingScreen to TrackerScreen. (Issue #139). **Status: Implemented (Deferred UI Hydration).**
*   **Settings Overlay ANR Remediation (R137)**: (Added Aug.10.32) The system MUST eliminate the 3000ms+ Main-thread stall occurring during the transition to the Settings overlay. (Issue #137). **Status: Implemented.**
*   **Forensic Pulse Frequency Hardening (R134)**: (Added Aug.10.29) The system MUST implement a high-frequency "Forensic Pulse" (10,000ms) for auditing resource-critical correlations (Issue #134-Sentinel). **Status: Implemented & Verified.**

### 2. Temporal & Forensic Integrity
*   **Temporal Forensic Integrity (R102)**: Monotonic `rt` for logic; wall-clock `ts` for logs. (Issue #102)
*   **Forensic Parity Authority (R118)**: Strict field parity across Protobuf, Database, and UI. (Issue #118)

### 3. UI/UX & Localization Authority
*   **Event & Alert Text Authority (R747)**: (Added Aug.07.06) Viewer-local events MUST be prefixed with "**This device:**". Tracker-remote events MUST omit the "**Tracker:**" prefix. (Issue #747)

### 4. Documentation & Integrity Governance
*   **Status Tracking Integrity (R752)**: (Added Aug.07.06) All status tracking documents MUST be synchronized after each resolution. (Issue #752)
*   **Historical Traceability (R749)**: (Added Aug.07.06) The historical resolution record MUST be synchronized across `issues.md` and `RESOLUTION_ARCHIVE.md`. (Issue #749)

### 5. Version Authority
*   **Current Release**: Aug.11.08.
*   **Source of Truth**: app/build.gradle versionName.
