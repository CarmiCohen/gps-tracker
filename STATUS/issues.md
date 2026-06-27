# Hardening Phase: Primary Tracking Document (v8.9.41)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 0 |
| **Validation Tasks** | 🟡 Pending Hardware | 0 |
| **Resolved (this phase)** | 🟢 Archived | 67 |

---

## 🔴 Open Issues (Contradictions & Inconsistencies)
*   *No open issues in this category.*

---

## 🟡 Open Issues (Hardening)
*   *No open issues in this category.*

---

## 🟢 Resolved (this phase)
*   **FIXED Samsung A15 Proximity Limitation (Issue #340)** - Resolution: Implemented Lux-Locked Proximity Gating in `AppSensorManager`. Suppressing "Far" transitions in 0 lux to mitigate virtual sensor failure in darkness. (Formerly #180)
*   **FIXED SNR-IMU Correlation Validation (Issue #332)** - Resolution: Refined `isVisualJump` in `PhysicsUtils` to penalize high SNR jumps without physical motion. Implemented specific Urban Canyon heuristics for spoofing detection. (Formerly #219)
*   **FIXED Acoustic "Location Pending" Optimization (Issue #328)** - Resolution: Implemented Velocity-Aware Bayesian Expansion in `MapComponents`. Uncertainty growth now scales with last known speed (1.5m/s floor). (Formerly #221)
*   **FIXED Physical Hardware Validation (Issue #341)** - Resolution: Implemented GPS Stability Audit in `TrackerService`. Monitoring 10Hz intervals and logging "STABILITY GAP" if > 200ms. Added periodic reliability percentage reporting.
*   **FIXED Xiaomi MIUI 14 Heuristic Recovery (Issue #342)** - Resolution: Hardened "Heuristic Recovery Pulse" in `TrackerService`. Detecting tick gaps > 15s to trigger forced GNSS revival and FGS type toggle.
*   **FIXED Hindsight Trajectory Correction (Issue #334)** - Resolution: Implemented "Rubber-Band" interpolation logic in `PhysicsUtils` to fill gaps during trajectory promotion. Ensured temporal continuity by adding `ts` to `EngineGeoPoint`. (Formerly #220)
*   **FIXED Hindsight Transition Smoothing (Issue #327)** - Resolution: Refined `LocationProcessor` to inject interpolated segments between last valid fix and promoted hindsight window, eliminating visual "teleporting." (Formerly #227)
*   **FIXED Forensic Log Enrichment (Issue #333)** - Resolution: Implemented auto-enrichment in `LogManager` to populate SNR and Vibration snapshots from latest telemetry if null. Verified parity across Local DB, Relay payloads, and UI.
*   **FIXED Intelligent Uncertainty UX Mapping (Issue #326)** - Resolution: Synchronized `locationPendingReason` propagation in `TrackerService` and `ViewerService` alarm evaluation loops. Verified UI display parity across Map and Status Bar layers.
*   All items from this phase have been moved to **[issues_archive.md](issues_archive.md)** for audit trail preservation.
