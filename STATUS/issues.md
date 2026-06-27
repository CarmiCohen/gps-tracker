# Hardening Phase: Primary Tracking Document (v8.9.38)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 1 |
| **Validation Tasks** | 🟡 Pending Hardware | 2 |
| **Resolved (this phase)** | 🟢 Archived | 64 |

---

## 🔴 Open Issues (Contradictions & Inconsistencies)

### 1. A15 Optical Proximity Limitation (Issue #340)
*   **Description**: As the Samsung A15 utilizes a virtual (optical) proximity sensor, it may fail to detect "Near" state in complete darkness (0 lux), potentially leading to false tamper alerts when handled in the dark. Logic hardened with debounce, but hardware limitation persists. (Formerly #180)
*   **Status**: Documented Risk.

---

## 🟡 Open Issues (Hardening)

### 1. Adaptive Jump Confidence & Spoofing Detection (Issue #332)
*   **Description**: Implementation of SNR-IMU correlation is complete; requires validation against real-world signal reflection scenarios (Urban Canyons). (Formerly #219)
*   **Status**: **Pending Validation**.

### 2. Acoustic "Location Pending" Optimization (Issue #328)
*   **Description**: Bayesian Confidence Scaling refinement for UI transitions. Ensure confidence radius correctly reflects time-since-last-fix. (Formerly #221)
*   **Status**: **Pending Validation**.

---

## 🟢 Resolved (this phase)
*   **FIXED Physical Hardware Validation (Issue #341)** - Resolution: Implemented GPS Stability Audit in `TrackerService`. Monitoring 10Hz intervals and logging "STABILITY GAP" if > 200ms. Added periodic reliability percentage reporting.
*   **FIXED Xiaomi MIUI 14 Heuristic Recovery (Issue #342)** - Resolution: Hardened "Heuristic Recovery Pulse" in `TrackerService`. Detecting tick gaps > 15s to trigger forced GNSS revival and FGS type toggle.
*   **FIXED Hindsight Trajectory Correction (Issue #334)** - Resolution: Implemented "Rubber-Band" interpolation logic in `PhysicsUtils` to fill gaps during trajectory promotion. Ensured temporal continuity by adding `ts` to `EngineGeoPoint`. (Formerly #220)
*   **FIXED Hindsight Transition Smoothing (Issue #327)** - Resolution: Refined `LocationProcessor` to inject interpolated segments between last valid fix and promoted hindsight window, eliminating visual "teleporting." (Formerly #227)
*   **FIXED Forensic Log Enrichment (Issue #333)** - Resolution: Implemented auto-enrichment in `LogManager` to populate SNR and Vibration snapshots from latest telemetry if null. Verified parity across Local DB, Relay payloads, and UI.
*   **FIXED Intelligent Uncertainty UX Mapping (Issue #326)** - Resolution: Synchronized `locationPendingReason` propagation in `TrackerService` and `ViewerService` alarm evaluation loops. Verified UI display parity across Map and Status Bar layers.
*   All items from this phase have been moved to **[issues_archive.md](issues_archive.md)** for audit trail preservation.
