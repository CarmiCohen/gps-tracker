# Hardening Phase: Primary Tracking Document (v8.9.37)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[compliance.md](compliance.md)** archive.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 6 |
| **Validation Tasks** | 🟡 Pending Hardware | 10 |
| **Resolved (this phase)** | 🟢 Archived | 45 |

---

## 🔴 Open Issues (Contradictions & Inconsistencies)

### 1. Network Integrity & Timeout Scaling (Issue #273)
*   **Description**: RTT scaling and timeout handling in `SyncManager.kt` requires refinement to prevent premature disconnects during poor network transitions. Proto references also need renumbering.
*   **Location**: `SyncManager.kt`, `EngineConstants.kt`, `app_settings.proto`.

### 2. Unified Accuracy Fallback Logic (Issue #214)
*   **Description**: Audit all logging and UI components to ensure they prioritize engine-calculated `maxAccuracy` over raw Android location accuracy consistently.
*   **Location**: `LogManager.kt`, `SharedUiComponents.kt`, `Models.kt`.

### 3. SIT Rising-Edge & Offline Context (Issue #244/245)
*   **Description**: Hardening of SIT detection triggers to prevent duplicate events and ensuring `locationPendingReason` is correctly captured in `PendingStatusEntity` for offline uncertainty context.
*   **Location**: `RemoteHandler.kt`, `Database.kt`, `HistoryManager.kt`.

### 4. New Phase Renumbering Cleanup (Issue #305)
*   **Description**: Final sweep to replace legacy low-number issue references (#1–#31) with renumbered (+270 offset) IDs in pending files to ensure audit traceability.
*   **Location**: `app_settings.proto`, `TelemetryAggregator.kt`, `AppSettingsMigration.kt`, `REQUIREMENTS_SOT.md`.

### 5. Shadow Constants Remediation (Issue #306)
*   **Description**: Physical removal of localized "Shadow Constants" and magic numbers identified during the SoT alignment (#281). 
*   **Location**: `TrackerStateManager.kt`, `AppSensorManager.kt`.

### 6. A15 Optical Proximity Limitation (Issue #450)
*   **Description**: As the Samsung A15 utilizes a virtual (optical) proximity sensor, it may fail to detect "Near" state in complete darkness (0 lux), potentially leading to false tamper alerts when handled in the dark. Logic hardened with debounce, but hardware limitation persists.
*   **Status**: Documented Risk.

---

## 🟡 Open Issues (Hardening)

### 1. Physical Hardware Validation: Xiaomi & Samsung (Issue #262)
*   **Description**: Verification of zero-spike behavior during transition and 10Hz polling stability.
*   **Status**: **Pending Validation**.

### 2. Xiaomi MIUI 14 Heuristic Recovery (Issue #218)
*   **Description**: Validate the "Xiaomi-Specific Pulse" logic to ensure it successfully wakes the system from deep Doze without excessive battery drain.
*   **Status**: **Pending Hardware Verification**.

### 3. Adaptive Jump Confidence & Spoofing Detection (Issue #219)
*   **Description**: Implementation of SNR-IMU correlation is complete; requires validation against real-world signal reflection scenarios (Urban Canyons).
*   **Status**: **Pending Validation**.

### 4. Hindsight Trajectory Correction (Issue #220)
*   **Description**: Verify "Rubber-Band" logic for retroactive trail smoothing when a rejected jump is later promoted to a valid trajectory. (Includes **Issue #227** smoothing refinement).
*   **Status**: **Pending Code Audit/Validation**.

### 5. Documentation Hardening (Issue #276)
*   **Description**: Final sweep of all `.md` files to ensure synchronization with v8.9.37 architecture and renumbered issues.
*   **Status**: **Ongoing**.

### 6. Acoustic "Location Pending" Optimization (Issue #221)
*   **Description**: Bayesian Confidence Scaling refinement for UI transitions. Ensure confidence radius correctly reflects time-since-last-fix.
*   **Status**: **Pending Validation**.

### 7. Intelligent Uncertainty UX Mapping (Issue #226)
*   **Description**: Verify consistent propagation and display of `locationPendingReason` across all UI layers and remote handlers.
*   **Status**: **Pending Audit**.

### 8. Forensic Log Enrichment (Issue #241)
*   **Description**: Audit SNR and Vibration snapshots in forensic logs to ensure parity between local database and relay payloads.
*   **Status**: **Pending Code Audit**.

### 10. Hindsight Transition Smoothing (Issue #227)
*   **Description**: Specific validation of `promotedPoints` injection into the trajectory stream to prevent visual "teleporting."
*   **Status**: **Pending Validation**.

---

## 🟢 Resolved (this phase)
*   **Issue #148**: Samsung A15 GPS Stalling. Enforced 1000ms polling heartbeat and implemented active WakeLock renewal on every service tick. Prevents aggressive OEM background GNSS suspension and ensures 100% background persistence on A15 hardware. (Complete)
*   **Issue #191**: Samsung A15 Proximity Flutter. Implemented a 500ms post-sync muzzle hysteresis window to filter virtual proximity sensor noise triggered by LED/Network I/O activity. Eliminates false tamper alerts during active telemetry synchronization. (Complete)
*   **Issue #190**: Xiaomi Autostart & Boot Resilience. Implemented robust handling for indeterminate "Unknown" status and `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient boot alarms. (Complete)
*   **Issue #304**: Tier 3 Jump Floor Contradiction. Resolved by updating `PhysicsUtils.kt` and `LocationSentinel.kt` to use `JUMP_GATE_VISUAL_JITTER_METERS` (10.0m).
*   **Issue #302**: Behavioral Magic Numbers. Resolved by moving thresholds to `EngineConstants.kt`.
*   **Issue #303**: Trajectory Gating Multiplier. Resolved by unifying `TRAJECTORY_REJECTION_ACCURACY_MULT`.
*   **Issue #301**: Vibration Threshold Inconsistency. Unified with `VIBRATION_STATIONARY_THRESHOLD`.
*   **Issue #285**: GtoEngine Implementation. Implemented sliding-window factor graph logic as per `GTO_ENGINE_SPEC.md`. (Formerly #15)
*   **Issue #279**: Foreground Resilience Hardening. (Formerly #9)
*   **Issue #281**: SoT Naming Alignment. (Formerly #11)
*   **Issue #296**: `serviceStartRealtime` Initialization Gap. (Formerly #26)
*   **Issue #294**: Viewer Offline Detection Logic Gap. (Formerly #24)
*   **Issue #288**: Vertical Displacement Failure. Corrected bridging between `AppSensorManager` and `LocationSentinel`. (Formerly #18)
*   **Issue #289**: Dead State Cleanup (Revival Flag). (Formerly #19)
*   **Issue #286**: Hardcoded EMA Cleanup. Replaced with `LUX_EMA_FAST`. (Formerly #16)
*   **Issue #263**: EMA Constant Inversion Audit. Corrected inverted weights in `EngineConstants.kt`.
*   **Issue #264**: GtoEngine Magic Number Consolidation. Moved thresholds to `EngineConstants.kt`.
*   **Issue #265**: TrackerService Redundant Evaluation Audit. Optimized `TrackerService.kt`.
*   **Issue #266**: Lux EMA Implementation Omission. Integrated Slow/Fast variants in `LocationSentinel.kt`.
*   **Issue #267**: Dead Code Cleanup: `isRevivalTriggered`. Removed unused flag in `TrackerService.kt`.
*   **Issue #268**: Acoustic Floor Logic Redundancy. Removed redundant parameter passing in `LocationProcessor.kt`.
*   **Issue #271**: Uptime Consistency. Consolidated redundant session timing fields into `uptimeMs`. (Formerly #1)
*   **Issue #272**: Battery Profile. Implemented discharge profiling in `app_settings.proto`. (Formerly #2)
*   **Issue #282**: SIT Duplicate Guard. Implemented persistent 15s sanity check in `HistoryManager.kt`. (Formerly #12)
*   **Issue #292**: Acoustic Floor Decay Logic. Fixed by enforcing `ACOUSTIC_FLOOR_MIN_DB = 25.0`. (Formerly #22)
*   **Issue #284**: Light EMA Logic Inconsistency. (Formerly #14)
*   **Issue #287**: Role-Aware Alert Title Visibility. (Formerly #17)
*   **Issue #295**: Redundant Barometric Baselining. (Formerly #25)
*   **Issue #291**: SIT Forensic Duplicate Risk. (Formerly #21)
*   **Issue #293**: Geofence Evaluation Bug (Viewer). (Formerly #23)
*   **Issue #297**: Hindsight Promotion Coverage. (Formerly #27)
