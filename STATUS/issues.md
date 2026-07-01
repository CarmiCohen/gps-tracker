# Hardening Phase: Primary Tracking Document (v8.9.68)

This document tracks all open issues, technical debt, and pending validation tasks. Once an item is verified, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 Medium | 2 |
| **Validation Tasks** | 🟡 Pending | 1 |
| **Resolved (this session)** | 🟢 Progress | 18 |
| **Archived Resolutions** | 📁 Historical | 260 |

---

## ⚠️ Newly Identified Risks & Concerns (v8.9.68)
*   **A15 Virtual Sensor Artifacts**: Hardened via #010. Implementation of #011 (Forensic Labeling) is recommended to verify suppression efficacy.

---

## 🔴 Open Technical Issues
| ID | Severity | Issue | Description |
| :--- | :--- | :--- | :--- |
| #011 | **Low** | **Suppression Forensic Labeling** | Log service events when hardware-specific muzzles (e.g. A15 gate) suppress a sensor violation. |
| #012 | **Low** | **Adaptive Proximity Debounce** | Scale proximity debounce based on CPU load and stationary duration to mitigate A15 virtual sensor flutter. |

## 🟡 Pending Validation (Hardening Phase)
*   **Off-Main-Thread Sensor Stability**: Verify long-term stability of the `AppSensorThread` under sustained high-frequency sampling.

## 🟢 Resolved (this session)
| ID | Severity | Issue | Status | Resolution |
| :--- | :--- | :--- | :--- | :--- |
| #010 | **Medium** | **A15 Acoustic/Vibration Coherence** | Resolved | Implemented coherence check in both Fast Path (`AppSensorManager`) and behavioral validation (`SentinelValidator`). Acoustic spikes on A15 are ignored if vibration is < 0.01g. |
| #013 | **High** | **A15 GPS Heartbeat Audit** | Resolved | Implemented "Warm Handoff" logic. Injects `force_time_injection` extra commands during stationary-to-moving transitions on A15 to prevent background clock stalling. |
| #009 | **High** | **A15 Forensic Isolation Spikes** | Resolved | Hardened sensor logic for Samsung A15: Increased acoustic jump threshold to 55dB, optimized light EMA factors, and muzzled confirmed noise artifacts. |
| #R325 | **Low** | **Samsung A15 Accuracy Truncation** | Resolved | Optimized layout width constraints to ensure authoritative uncertainty display fits narrow screens. |
| #006 | **High** | **Samsung A15 Main Thread Jitter** | Resolved | Offloaded `onSensorChanged` processing to `AppSensorThread` (HandlerThread) to eliminate UI thread blocking. |
| #007 | **Medium** | **Connectivity Rejoin Latency** | Resolved | Implemented reactive `ConnectionLostCallback` for immediate `wakeUpRelay()` and enforced `websocket` transport. |
| #008 | **Low** | **VID_NOTES Correction** | Resolved | Updated note identifier to "renumv". |
| #461 | **Medium** | **Settings Uniqueness UI Feedback** | Resolved | Implemented error propagation from `SettingsRepository` to UI via Toast. |
| #001 | **High** | **Room Schema Divergence** | Resolved | Incremented DB to v51 and corrected migrations. |
| #002 | **Medium** | **GPS Status UI Mismatch** | Resolved | Increased failure thresholds to 35s to accommodate 20s stationary polling. |
| #003 | **Medium** | **Main Thread Jitter (Davey)** | Resolved | Moved state computations to `Dispatchers.Default` in `MainViewModel`. |
| #004 | **Low** | **A15 Virtual Proximity Suppression** | Resolved | Refined manager to allow 'Far' transitions in darkness during motion. |
| #005 | **Low** | **Map Provider Log Spillage** | Resolved | Silenced `osmdroid` debug logs and enforced static user agent. |
| #458 | **Medium** | **Tracker Role Ghost Mode Bug** | Resolved | Corrected role-aware timestamp propagation in `GlobalStatusBar`. |
| #459 | **Low** | **Unicode Escape Regression** | Resolved | Resolved double-escaping in string literals for thin-space rendering. |
| #460 | **Low** | **Local Freshness Existence Logic** | Resolved | Relaxed existence check to include sensor telemetry. |
