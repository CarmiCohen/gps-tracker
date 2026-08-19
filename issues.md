# Project Issues & Hardening Tracking (Aug.18.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Active | 2 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 648 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #204-C1**: Diagnostic down-sampling reduces forensic fidelity (4Hz vs 100Hz). This is a temporary state for stress-isolation and must be reverted before production release.
*   **Concern #207-C1 (Risk)**: Residual Main Thread Pressure. "Davey" logs (1s+) persist even at 4Hz sampling. (Issue #207)
*   **Concern #208-C1 (Defect)**: Excessive GC & Heap Fragmentation. Logcat reveals near-constant mark-compact GC cycles (~1/sec) taking 100ms+. This indicates high object churn in the UI layer (ViewModel/Compose) independent of forensic logging frequency. (Issue #208)

---

## 🔴 Open Issues
*   **Issue #208: UI Layer Allocation Audit**: Identify and eliminate high-churn object allocations in `MainViewModel` and `AppMapContainer` that are triggering constant GC cycles.
*   **Issue #207: Main-Thread Bottleneck Audit**: Identify the root cause of large frame hangs (1s+) that persist during low-frequency sampling. Focus on Map rendering and database contention.

---

## 🟢 Recently Resolved Issues (Aug.18.08)
*   **Issue #206: Samsung-Specific Permission Navigation**: Implemented fallback logic in `MainActivity.kt` for `ACTION_MANAGE_OVERLAY_PERMISSION` to handle intent URI rejections on Samsung A15/API 35 (R206).
*   **Issue #205: UI String Rendering Artifacts**: Forced LTR layout direction in `SettingsComponents.kt` for technical telemetry screens to resolve BiDi mirroring and punctuation artifacts (R205).
*   **Issue #204: Diagnostic Stress Isolation (Sensor Sampling Rates)**: Implemented temporary diagnostic down-sampling (4Hz/2Hz) to isolate high-frequency overhead (R204).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.18.08)
