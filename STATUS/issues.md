# Hardening Phase: Primary Tracking Document (v8.9.63)

This document tracks all open issues, technical debt, and pending validation tasks. Once an item is verified, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clear | 0 |
| **Validation Tasks** | 🟡 Pending | 1 |
| **Resolved (this session)** | 🟢 Progress | 11 |
| **Archived Resolutions** | 📁 Historical | 260 |

---

## ⚠️ Newly Identified Risks & Concerns (v8.9.63)
*   *(None)*

---

## 🔴 Open Technical Issues
*   *(None)*

## 🟡 Pending Validation (Hardening Phase)
*   **Validation of R325 Side-by-Side Accuracy**: Ensure the `(±Xm)` authoritative uncertainty display doesn't overlap or truncate on narrow devices (e.g., Samsung A15).

## 🟢 Resolved (this session)
| ID | Severity | Issue | Status | Resolution |
| :--- | :--- | :--- | :--- | :--- |
| #461 | **Medium** | **Settings Uniqueness UI Feedback** | Resolved | Implemented error propagation from `SettingsRepository` to `MainViewModel`. Errors (e.g., ID collision) are now displayed via Toast. |
| #001 | **High** | **Room Schema Divergence** | Resolved | Incremented DB to v51. Corrected historical migrations to ensure schema consistency. |
| #002 | **Medium** | **GPS Status UI Mismatch** | Resolved | Increased failure thresholds to 35s to accommodate 20s stationary polling. |
| #003 | **Medium** | **Main Thread Jitter (Davey)** | Resolved | Moved state computations to `Dispatchers.Default` in `MainViewModel`. |
| #004 | **Low** | **A15 Virtual Proximity Suppression** | Resolved | Refined `AppSensorManager` to allow 'Far' transitions in darkness during motion. |
| #005 | **Low** | **Map Provider Log Spillage** | Resolved | Silenced `osmdroid` debug logs and enforced static user agent. |
| #458 | **Medium** | **Tracker Role Ghost Mode Bug** | Resolved | Corrected role-aware timestamp propagation in `GlobalStatusBar`. |
| #459 | **Low** | **Unicode Escape Regression** | Resolved | Resolved double-escaping in string literals for thin-space rendering. |
| #460 | **Low** | **Local Freshness Existence Logic** | Resolved | Relaxed existence check to include sensor telemetry. |
