# Project Issues & Hardening Tracking (v8.9.87)

This document tracks active issues, technical debt, and pending validation tasks. Historical resolutions are moved to the [Issues Archive](STATUS/issues_archive.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 High | 0 |
| **Validation Tasks** | 🟡 Pending | 6 |
| **Resolved (Total)** | 🟢 Progress | 34 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Proto Schema Duplication**: Identical `.proto` files exist in `app/src/main/proto` and `app/src/proto`. Risks synchronization drift.
*   **Soak Test Monitoring**: Ongoing 24-hour stability test required to monitor for `STABILITY GAP` logs under 10Hz sensor load.
*   **UI Refresh Consistency**: Verify forensic fields (`Prox Debounce`, `Rolling Vibe`) respect the 15s staleness gate.
*   **SettingsUseCase Type Mismatch**: `SettingsUseCase.loadAllSettings` was found using `getFloat` for `Double`-backed distance and temperature keys. Fixed in v8.9.87.

---

## 🔴 Open Issues
*   *(No open critical issues)*

---

## 🟡 Pending Validation
*   **Issue #005 Verification**: Confirm logcat is silent on G990/A155 regarding `getPackageName` spam during map interactions.
*   **Identity Persistence**: Verify Viewer ID correctly persists as "V" (and user-defined values) without reverting to "T" on fresh installs or settings saves.
*   **Proto precision upgrade**: Verify that existing `max_distance` and `max_accuracy` values are correctly interpreted in the UI.
*   **Off-Main-Thread Sensor Stability**: Verify long-term stability of the `AppSensorThread`.
*   **Stationary Scaling Efficacy**: Confirm that `PROXIMITY_STARY_SCALING_MS_PER_HOUR` correctly scales skepticism.
*   **Issue #025 Transition Verification**: Perform unattended physical tamper tests to verify FGS type escalation on Android 14+.

---

## 🟢 Recently Resolved Issues (v8.9.87)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#005** | **Map Provider Log Spillage** | **Resolved**. Hardened remediation by forcing a static user agent string ("GpsTracker/8.9.87") in `GpsApplication`. This eliminates repetitive `getPackageName()` calls that triggered system log spam on Samsung devices. |
| **ID-01** | **Viewer ID Identity Reversion** | **Resolved**. Fixed logic error in `SettingsRepository` where `viewerIdFlow` incorrectly defaulted to Tracker ID ("T"). Corrected `commitDraftSettings` to properly apply `draftRelayUrl` and hardened uniqueness checks using effective IDs. |
| **#025** | **FGS Transition Timeout** | **Resolved (v8.9.86)**. Increased `UI_PULSE_TIMEOUT_MS` to 45s. |

**Full history available in [STATUS/issues_archive.md](STATUS/issues_archive.md).**
