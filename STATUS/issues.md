# Hardening Phase: Primary Tracking Document (v8.9.71)

This document tracks all open issues, technical debt, and pending validation tasks. Once an item is verified, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 Medium | 1 |
| **Validation Tasks** | 🟡 Pending | 3 |
| **Resolved (this session)** | 🟢 Progress | 21 |
| **Archived Resolutions** | 📁 Historical | 260 |

---

## ⚠️ Newly Identified Risks & Concerns (v8.9.71)
*   **Soak Test Monitoring**: Ongoing 24-hour stability test required to monitor for `STABILITY GAP` logs under 10Hz sensor load.
*   **UI Refresh Consistency**: Need to verify forensic fields (`Prox Debounce`, `Rolling Vibe`) respect the 15s staleness gate in low-signal conditions.

---

## 🔴 Open Technical Issues
| ID | Severity | Issue | Description |
| :--- | :--- | :--- | :--- |
| #014 | **Low** | **Type Safety / Conversion Optimization** | Numerous `toDouble()`/`toFloat()` conversions in the telemetry chain (identified via grep) could be cleaned up by standardizing property types across the `core:engine` and `app` modules. |

## 🟡 Pending Validation (Hardening Phase)
*   **Off-Main-Thread Sensor Stability**: Verify long-term stability of the `AppSensorThread` under sustained high-frequency sampling.
*   **Stationary Scaling Efficacy**: Confirm that `PROXIMITY_STARY_SCALING_MS_PER_HOUR` correctly scales skepticism without false negatives.
*   **A15 Forensic Suppression**: Verify `suppressionNote` visibility in the log sink during real-world A15 interference.

## 🟢 Resolved (this session)
| ID | Severity | Issue | Status | Resolution |
| :--- | :--- | :--- | :--- | :--- |
| #011 | **Low** | **Suppression Forensic Labeling** | Resolved | Implemented `suppressionNote` generation in `LocationSentinel` and logging in `TrackerService`. |
| #012 | **Low** | **Adaptive Proximity Debounce** | Resolved | Implemented stationary and stress-based scaling in `AppSensorManager`. |
| #013 | **High** | **Forensic UI Expansion** | Resolved | Exposed internal scaling metrics to the UI dashboard and propagated them through the sync manager. |
| #010 | **Medium** | **A15 Acoustic/Vibration Coherence** | Resolved | Implemented coherence check in both Fast Path (`AppSensorManager`) and behavioral validation (`SentinelValidator`). |
| #R325 | **Low** | **Samsung A15 Accuracy Truncation** | Resolved | Optimized layout width constraints to ensure authoritative uncertainty display fits narrow screens. |
| #006 | **High** | **Samsung A15 Main Thread Jitter** | Resolved | Offloaded `onSensorChanged` processing to `AppSensorThread` (HandlerThread). |
| #007 | **Medium** | **Connectivity Rejoin Latency** | Resolved | Implemented reactive `ConnectionLostCallback` for immediate `wakeUpRelay()`. |
| #461 | **Medium** | **Settings Uniqueness UI Feedback** | Resolved | Implemented error propagation from `SettingsRepository` to UI via Toast. |
| #001 | **High** | **Room Schema Divergence** | Resolved | Incremented DB to v51 and corrected migrations. |
| #002 | **Medium** | **GPS Status UI Mismatch** | Resolved | Increased failure thresholds to 35s to accommodate 20s stationary polling. |
| #003 | **Medium** | **Main Thread Jitter (Davey)** | Resolved | Moved state computations to `Dispatchers.Default` in `MainViewModel`. |
| #004 | **Low** | **A15 Virtual Proximity Suppression** | Resolved | Refined manager to allow 'Far' transitions in darkness during motion. |
| #005 | **Low** | **Map Provider Log Spillage** | Resolved | Silenced `osmdroid` debug logs and enforced static user agent. |
| #458 | **Medium** | **Tracker Role Ghost Mode Bug** | Resolved | Corrected role-aware timestamp propagation in `GlobalStatusBar`. |
| #459 | **Low** | **Unicode Escape Regression** | Resolved | Resolved double-escaping in string literals for thin-space rendering. |
| #460 | **Low** | **Local Freshness Existence Logic** | Resolved | Relaxed existence check to include sensor telemetry. |
