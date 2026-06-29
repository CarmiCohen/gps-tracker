# Hardening Phase: Primary Tracking Document (v8.9.60)

This document tracks all open issues, technical debt, and pending validation tasks. Once an item is verified, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 Low | 1 |
| **Validation Tasks** | 🟡 Pending | 1 |
| **Resolved (this session)** | 🟢 Progress | 3 |
| **Archived Resolutions** | 📁 Historical | 259 |

---

## ⚠️ Newly Identified Risks & Concerns (v8.9.60)
*   *(None. Forensic Ghost Mode risks resolved)*

---

## 🔴 Open Technical Issues
*   **Issue #461: Settings Uniqueness UI Feedback**: Verify/Implement error propagation from `SettingsRepository.commitDraftSettings` to the UI to prevent silent save failures.

## 🟡 Pending Validation (Hardening Phase)
*   **Validation of R325 Side-by-Side Accuracy**: Ensure the `(±Xm)` authoritative uncertainty display doesn't overlap or truncate on narrow devices.

## 🟢 Resolved (this session)
*   **FIXED Issue #458: Tracker Role Ghost Mode Bug**: Corrected role-aware timestamp propagation in `GlobalStatusBar`.
*   **FIXED Issue #459: Unicode Escape Regression**: Resolved double-escaping in string literals for thin-space rendering.
*   **FIXED Issue #460: Local Freshness Existence Logic**: Relaxed existence check to include sensor telemetry.
