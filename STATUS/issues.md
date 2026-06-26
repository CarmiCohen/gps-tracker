# Hardening Phase: Primary Tracking Document (v8.9.37)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[compliance.md](compliance.md)** archive.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 2 |
| **Validation Tasks** | 🟡 Pending Hardware | 8 |
| **Resolved (this phase)** | 🟢 Archived | 52 |

---

## 🔴 Open Issues (Contradictions & Inconsistencies)

### 1. SIT Rising-Edge & Offline Context (Issue #244/245)
*   **Description**: Hardening of SIT detection triggers to prevent duplicate events and ensuring `locationPendingReason` is correctly captured in `PendingStatusEntity` for offline uncertainty context.
*   **Location**: `RemoteHandler.kt`, `Database.kt`, `HistoryManager.kt`.

### 2. A15 Optical Proximity Limitation (Issue #180)
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

### 5. Acoustic "Location Pending" Optimization (Issue #221)
*   **Description**: Bayesian Confidence Scaling refinement for UI transitions. Ensure confidence radius correctly reflects time-since-last-fix.
*   **Status**: **Pending Validation**.

### 6. Intelligent Uncertainty UX Mapping (Issue #226)
*   **Description**: Verify consistent propagation and display of `locationPendingReason` across all UI layers and remote handlers.
*   **Status**: **Pending Audit**.

### 7. Forensic Log Enrichment (Issue #241)
*   **Description**: Audit SNR and Vibration snapshots in forensic logs to ensure parity between local database and relay payloads.
*   **Status**: **Pending Code Audit**.

### 8. Hindsight Transition Smoothing (Issue #227)
*   **Description**: Specific validation of `promotedPoints` injection into the trajectory stream to prevent visual "teleporting."
*   **Status**: **Pending Validation**.

---

## 🟢 Resolved (this phase)
*   **Issue #307**: Requirements Conflict Audit. Resolved all documentation and implementation contradictions for R922 (LEDs), R925 (Timing), and R810 (IDs). (Complete)
*   **Issue #321**: Shadow Constants Remediation. Replaced localized magic numbers with EngineConstants references inside AppSensorManager and TrackerStateManager. (Formerly #306) (Complete)
*   **Issue #322**: Architectural Bloat: ViewModel Decoupling. Decoupled MainViewModel by moving modular tracking code into TelemetryUseCase. (Formerly #115) (Complete)
*   **Issue #305**: New Phase Renumbering Cleanup. Completed final sweep and replaced low-number or double-offset references across `TelemetryAggregator.kt`, `EngineConstants.kt`, `RemoteHandler.kt`, and documentation files to ensure exact single-hop audit traceability below the #350 baseline limit. (Complete)
*   **Issue #214**: Unified Accuracy Fallback Logic. Audited all logging and UI components to ensure prioritization of engine-calculated `maxAccuracy`.
*   **Issue #273**: Network Integrity & Timeout Scaling. Refined RTT scaling and synchronized `app_settings.proto` fields.
*   **Issue #312**: Documentation Hardening. Final sweep of all `.md` files to ensure synchronization with v8.9.37 architecture and renumbered issues.
*   **Issue #148**: Samsung A15 GPS Stalling. Enforced 1000ms polling heartbeat and implemented active WakeLock renewal on every service tick. Prevents aggressive OEM background GNSS suspension and ensures 100% background persistence on A15 hardware. (Complete)
*   **Issue #191**: Samsung A15 Proximity Flutter. Implemented a 500ms post-sync muzzle hysteresis window to filter virtual proximity sensor noise triggered by LED/Network I/O activity. Eliminates false tamper alerts during active telemetry synchronization. (Complete)
*   **Issue #190**: Xiaomi Autostart & Boot Resilience. Implemented robust handling for indeterminate "Unknown" status and `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient boot alarms. (Complete)
*   **Issue #304**: Tier 3 Jump Floor Contradiction. Resolved by updating `PhysicsUtils.kt` and `LocationSentinel.kt` to use `JUMP_GATE_VISUAL_JITTER_METERS` (10.0m).
*   **Issue #302**: Behavioral Magic Numbers. Resolved by moving thresholds to `EngineConstants.kt`.
*   **Issue #303**: Trajectory Gating Multiplier. Resolved by unifying `TRAJECTORY_REJECTION_ACCURACY_MULT`.
*   **Issue #301**: Vibration Threshold Inconsistency. Unified with `VIBRATION_STATIONARY_THRESHOLD`.
*   **Issue #285**: GtoEngine Implementation. Implemented sliding-window factor graph logic as per `GTO_ENGINE_SPEC.md`.
*   **Issue #279**: Foreground Resilience Hardening.
*   **Issue #281**: SoT Naming Alignment.
*   **Issue #296**: `serviceStartRealtime` Initialization Gap.
*   **Issue #294**: Viewer Offline Detection Logic Gap.
*   **Issue #288**: Vertical Displacement Failure. Corrected bridging between `AppSensorManager` and `LocationSentinel`.
*   **Issue #289**: Dead State Cleanup (Revival Flag).
*   **Issue #286**: Hardcoded EMA Cleanup. Replaced with `LUX_EMA_FAST`.
*   **Issue #263**: EMA Constant Inversion Audit. Corrected inverted weights in `EngineConstants.kt`.
*   **Issue #264**: GtoEngine Magic Number Consolidation. Moved thresholds to `EngineConstants.kt`.
*   **Issue #265**: TrackerService Redundant Evaluation Audit. Optimized `TrackerService.kt`.
*   **Issue #266**: Lux EMA Implementation Omission. Integrated Slow/Fast variants in `LocationSentinel.kt`.
*   **Issue #267**: Dead Code Cleanup: `isRevivalTriggered`. Removed unused flag in `TrackerService.kt`.
*   **Issue #268**: Acoustic Floor Logic Redundancy. Removed redundant parameter passing in `LocationProcessor.kt`.
*   **Issue #271**: Uptime Consistency. Consolidated redundant session timing fields into `uptimeMs`.
*   **Issue #272**: Battery Profile. Implemented discharge profiling in `app_settings.proto`.
*   **Issue #282**: SIT Duplicate Guard. Implemented persistent 15s sanity check in `HistoryManager.kt`.
*   **Issue #292**: Acoustic Floor Decay Logic. Fixed by enforcing `ACOUSTIC_FLOOR_MIN_DB = 25.0`.
*   **Issue #284**: Light EMA Logic Inconsistency.
*   **Issue #287**: Role-Aware Alert Title Visibility.
*   **Issue #295**: Redundant Barometric Baselining.
*   **Issue #291**: SIT Forensic Duplicate Risk.
*   **Issue #293**: Geofence Evaluation Bug (Viewer).
*   **Issue #297**: Hindsight Promotion Coverage.
