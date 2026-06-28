# Compliance & Operational Requirements (Audit Baseline)

This document serves as the formal proof of implementation for the GPS-Tracker system. It contains the Verification Manifest (Requirements Tracking) and recent Hardening Phase resolutions. 

**For historical resolutions (Issue #1 to #199), see [compliance_archive.md](compliance_archive.md).**

## 1. Verification Manifest (Requirement Status)

| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R568a** | **Last Relay Traffic Monotonic Timestamp**: Use of `elapsedRealtime` for zombie connection detection in `SignalingProvider`. | **Verified (v8.9.40)** |
| **R729** | **Behavioral Debouncing & Muzzle Hardening**: Unified timing gates for alert suppression (Issue #191). Includes Muzzle (2s), Hysteresis (500ms), and Proximity (5s). | **Verified (v8.9.40)** |
| **R730** | **Unified Vibration Floor Update (EMA)**: Dynamic floor tracking for environmental adaptation. | **Verified (v8.9.40)** |
| **R800** | **Unified Back Navigation**: Standardized `BackHandler` usage in `AlarmActivity` for consistent exit logic. | **Verified (v8.9.40)** |
| **R805** | **Map Marker Color Standardization**: Integration of Purple500 for marker categorization in `Color.kt`. | **Verified (v8.9.40)** |
| **R832** | **Chair Sit Detection Engine**: Multi-sensor fusion (tilt/vibration/baro) for occupancy detection. | **Verified (v8.9.40)** |
| **R853** | **Atomic HomePoint Updates**: Support for atomic bulk updates of home points in `SettingsRepository`. | **Verified (v8.9.40)** |
| **R854** | **Siren Master Control**: Unified UI grouping for siren alert management in `strings.xml`. | **Verified (v8.9.40)** |
| **R865** | **Unified Identity Green**: Branding color Green must be explicitly integrated as the primary color theme across backgrounds/status bars. | **Verified (Audit v8.9.40)** |
| **R866** | **Branding Accuracy**: JD Branding Green must match exactly #367C2B. | **Verified (Audit v8.9.40)** |
| **R880** | **Evidence-based Parking Exit**: Hardened behavioral state transitions in `TrackerStateManager`. | **Verified (v8.9.40)** |
| **R922** | **Role-aware LED Logic**: Tracker mode LEDs reflect local hardware health; Viewer mode LEDs gate by remote peer pulse health (Issue #224). | **Verified (v8.9.40)** |
| **R925** | **Landing Page Pause**: Mandatory 2,000ms pause during session auto-transition to ensure state stability (Issue #215). | **Verified (v8.9.40)** |
| **R926** | **Service Launch Integrity**: Background service is explicitly launched following the auto-transition delay (Issue #215). | **Verified (v8.9.40)** |
| **R944** | **Binary Signaling Efficiency**: Protobuf-based binary payload emission for signaling efficiency. | **Verified (v8.9.40)** |
| **R945** | **Reactive System State Flows**: Cold-to-hot reactive flows for hardware/system state monitoring. | **Verified (v8.9.40)** |

## 2. Resolution Archive

### 2.1. Hardening Phase Resolutions (v8.9.42)
*   **FIXED Issue Mapping Contradictions** - Resolution: Unified authoritative issue mapping across all source files and documentation.
    *   **Issue #335 Collision**: Disambiguated Vibration thresholds to **#318** and Service Initialization to **#335**.
    *   **Issue #245 Mapping**: Authoritatively assigned **#326** to Intelligent Uncertainty (locationPendingReason) and **#348** to SIT Rising-Edge logic.
    *   **Issue #272 Ambiguity**: Explicitly separated Thermal Throttling (**#352**) from Battery Health Profiling (**#353**).
    *   **Issue #282 Collision**: Assigned **#343** to Signal Loss Forensic Latching and consolidated SIT duplicate guards under **#336**.
*   **FIXED Requirement SOT Synchronization** - Resolution: Synchronized `requirements_sot.md` and `compliance.md` with implemented features found in source code and resources (R568a, R800, R805, R832, R853, R854, R880).
*   **FIXED Divergent Sub-requirements Segregation (R404) (#404)** - Resolution: Resolved ID collision where R404 was used for both binary signaling and reactive flows. Mapped binary signaling to **R944** and reactive system flows to **R945**. Updated `SignalingProvider.kt`, `SystemStatusProvider.kt`, and System SoT.
*   **FIXED Behavioral Description Overlap (R729/R730)** - Resolution: Resolved ID collision by segregating Behavioral Debouncing (R729) from Vibration EMA Floor updates (R730). Synchronized `EngineConstants.kt`, `SentinelValidator.kt`, and the System SoT.
*   **FIXED Branding Contradiction Audit (R865/R866)** - Resolution: Unified the "Unified Identity Green" to exactly #367C2B across all layers. Replaced deprecated Lime 500 in `ic_status_tractor.xml`, `Theme.kt`, and all UI components. Synchronized XML resources (`colors.xml`, `splash_background.xml`) to ensure 100% visual consistency.
*   **FIXED Physical Hardware Validation (Issue #341)** - Resolution: Implemented GPS Stability Audit in `TrackerService`. Monitoring 10Hz intervals and logging "STABILITY GAP" if > 200ms. Added periodic reliability percentage reporting.
*   **FIXED Forensic Pipeline Verification (Issue #180)** - Resolution: Verified 1:1 field mapping for verticalVelocity and SIT metadata during forensic telemetry injection.
*   **FIXED Uptime Consistency (Issue #357)** - Resolution: Consolidated redundant session timing fields into a single `uptimeMs` metric using monotonic `elapsedRealtime`. (Formerly #271)
*   **FIXED Integrity Mapping Ambiguity (#273)** - Resolution: Clarified that Issue #273 served as the architectural predecessor for both **Network Integrity (#315)** and **Storage Integrity (#316)**. Documentation updated to reflect the split.
*   **FIXED Documentation Hardening (#312)** - Resolution: Finalized Source of Truth alignment across all markdown files. Resolved description inconsistency where it was previously mislabeled as "Gating."
*   **FIXED R922: LED Logic Implementation (#224)** - Resolution: Formalized role-aware indicator logic. Tracker LEDs reflect local health; Viewer LEDs reflect peer pulse status to ensure end-to-end link verification visibility.
*   **FIXED Session Lifecycle Mapping (R925/R926) (#215)** - Resolution: Segregated the 2,000ms landing page pause (R925) from the background service launch sequence (R926) to ensure granular audit traceability.
