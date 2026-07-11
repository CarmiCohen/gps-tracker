# Project Issues & Hardening Tracking (v9.3.15)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 274 |

---

## ⚠️ Newly Identified Risks & Concerns
*No newly identified risks.*

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.15)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#077** | **Type Safety Hardening** | **Resolved**. Systematic audit and elimination of redundant `toDouble()`/`toFloat()` conversions across core engine and app modules. Implemented `Double` pre-buffering in `AppSensorManager` and captured boundary conversions at the source in `TrackerService`. Standardized all persistence to `Double` in `SettingsRepository`. |

---

## 🟢 Recently Resolved Issues (v9.3.14)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **C-068-1** | **Samsung System API Noise** | **Resolved**. Implemented comprehensive 10s TTL caching for all permission and system status checks in `SystemStatusProviderImpl.kt`. Switched high-frequency paths in `TrackerService` and `MainViewModel` to use cached states, eliminating Logcat jitter on Samsung G990/A15. |

---

## 🟢 Recently Resolved Issues (v9.3.13)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#062** | **Hardening** | **Resolved**. Implemented **Dynamic Anchor Breakout**: A displacement-weighted monitor in `LocationProcessor.kt` that uses trend analysis, velocity integration (IMM), and escape scoring to prevent "sticky anchors". (Validation: #053) |
