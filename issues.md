# Project Issues & Hardening Tracking (v8.9.86)

This document tracks all open issues, technical debt, and pending validation tasks. 

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 High | 0 |
| **Validation Tasks** | 🟡 Pending | 5 |
| **Resolved (Total)** | 🟢 Progress | 32 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Proto Schema Duplication**: Identical `.proto` files exist in `app/src/main/proto` and `app/src/proto`. This risks synchronization errors. Recommendation: Consolidate to a single source.
*   **Soak Test Monitoring**: Ongoing 24-hour stability test required to monitor for `STABILITY GAP` logs under 10Hz sensor load.
*   **UI Refresh Consistency**: Verify forensic fields (`Prox Debounce`, `Rolling Vibe`) respect the 15s staleness gate.

---

## 🔴 Open Issues
*   *(No open critical issues)*

---

## 🟡 Pending Validation
*   **Proto precision upgrade**: Verify that existing `max_distance` and `max_accuracy` values are correctly interpreted (Migration verified in v8.9.84).
*   **Off-Main-Thread Sensor Stability**: Verify long-term stability of the `AppSensorThread`.
*   **Stationary Scaling Efficacy**: Confirm that `PROXIMITY_STARY_SCALING_MS_PER_HOUR` correctly scales skepticism.
*   **Issue #025 Transition Verification**: Perform unattended physical tamper tests to verify FGS type escalation without `ForegroundServiceStartNotAllowedException`.

---

## 🟢 Resolved Issues

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#025** | **FGS Transition Timeout** | **Resolved (v8.9.86)**. Increased `UI_PULSE_TIMEOUT_MS` to 45s. This relaxes the user-interaction pulse gate, allowing automated state transitions (e.g., Physical Tamper -> Acoustic Monitor) to successfully claim Android 14+ FGS types when the device is unattended. |
| **#024** | **Accuracy Window Aliasing** | **Resolved (v8.9.85)**. Increased `ACCURACY_WINDOW_BUCKET_MS` to 120s (30s buckets). This ensures that stationary GPS fixes (received every 20s) do not immediately trigger new window buckets, allowing proper aggregation of maxAccuracy over a stable horizon. |
| **#023** | **DataStore Binary Incompatibility** | **Resolved (v8.9.84)**. Reverted tags 8, 9, 33 to float (legacy) and introduced new double tags 60, 61, 62 with a robust DataMigration in `SettingsRepository`. |
| **#022** | **Deep-Link Cold-Start Handling** | **Resolved (v8.8.6)**. Remapped from #44 to unify numbering. Implemented intent-aware startup for direct map navigation. |
| **#021** | **Map UI Infinite Loop** | **Resolved (v8.9.82)**. Fixed a logic error in `drawTrailToFolder` (`MapComponents.kt`) that caused an infinite loop on the main thread when processing single-point trail segments during property changes. |
| **#020** | **Map Centering Race Condition** | **Resolved (v8.9.83)**. Introduced `localLockStatus` in `MapComponents.kt` to immediately suspend centering logic upon touch detection, preventing the map from "fighting" user swiping/zooming gestures. |
| **#019** | **Android 14+ Permission Transition** | **Resolved**. Added UI pulse check and refined FGS type claims in `TrackerService`. |
| **#017** | **SnapshotStateList Lock Failures** | **Resolved (v8.9.81)**. Replaced observable `SnapshotStateList` pools in `MapComponents.kt` with standard `MutableList` to eliminate snapshot overhead in imperative update blocks. |
| **#014** | **Type Safety / Double Migration** | **Resolved (v8.9.80)**. Standardized all telemetry fields to `Double` across Proto, Services, Repositories, and UI State. Fixed signature mismatches in `TrackerService` and `SessionManager`. |
| **#016** | **Main Thread Performance Bottlenecks** | **Resolved**. Optimized trail rendering in `MapComponents.kt` (cached drawables, single-pass segment extraction) and offloaded startup I/O in `SettingsUseCase.kt`. |
| **#018** | **Tracker Behavior Stability (JUMPING)** | **Resolved**. Implemented "Hard-Lock" stationary anchoring logic in `LocationProcessor.kt`. |
| **#015** | **Coroutine Cancellation Noise** | **Resolved**. Hardened managers to ignore `CancellationException` in logs. |
| **#013** | **Forensic UI Expansion** | **Resolved**. Exposed internal scaling metrics to the dashboard and sync manager. |
| **#011** | **Suppression Forensic Labeling** | **Resolved**. Implemented `suppressionNote` in `LocationSentinel`. |
| **#012** | **Adaptive Proximity Debounce** | **Resolved**. Implemented scaling in `AppSensorManager`. |
| **#010** | **A15 Acoustic/Vibration Coherence** | **Resolved**. Implemented coherence check in Fast Path and validation. |
| **#R325** | **Samsung A15 Accuracy Truncation** | **Resolved**. Optimized layout width constraints for narrow screens. |
| **#006** | **Samsung A15 Main Thread Jitter** | **Resolved**. Offloaded `onSensorChanged` to `AppSensorThread`. |
| **#007** | **Connectivity Rejoin Latency** | **Resolved**. Implemented reactive `ConnectionLostCallback`. |
| **#461** | **Settings Uniqueness UI Feedback** | **Resolved**. Implemented error propagation to UI. |
| **#001** | **Room Schema Divergence** | **Resolved**. Incremented DB to v51/v52 and corrected migrations. |
| **#002** | **GPS Status UI Mismatch** | **Resolved**. Increased failure thresholds to 35s. |
| **#003** | **Main Thread Jitter (Davey)** | **Resolved**. Moved state computations to `Dispatchers.Default`. |
| **#004** | **A15 Virtual Proximity Suppression** | **Resolved**. Refined manager for 'Far' transitions in darkness. |
| **#005** | **Map Provider Log Spillage** | **Resolved**. Silenced `osmdroid` logs. |
| **#458** | **Tracker Role Ghost Mode Bug** | **Resolved**. Corrected role-aware timestamp propagation. |
| **#459** | **Unicode Escape Regression** | **Resolved**. Resolved double-escaping in string literals. |
| **#460** | **Local Freshness Existence Logic** | **Resolved**. Relaxed existence check to include sensor telemetry. |
