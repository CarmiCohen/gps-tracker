# Project Issues & Hardening Tracking (Aug.21.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 ACTIVE | 2 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 680 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #244-C1**: **Missing Native Binaries**: `libjdHardware.so` and `libmbrainSDK` failed to load. Investigating impact on Samsung A15 (R244).
*   **Concern #246-C1**: **High UI Latency (Davey)**: Observed 765ms frame hangs on Samsung A15 during telemetry bursts (R246).

---

## 🔴 Open Issues
*   **Issue #244**: **Native Library Audit**: Investigate and resolve `dlopen` failures for hardware abstraction libraries (R244).
*   **Issue #246**: **UI Thread Optimization**: Investigate and remediate Davey stalls (>700ms) observed on target hardware (R246).

---

## 🟢 Recently Resolved Issues (Aug.21.01)
*   **Issue #247**: **UI Component Regression**: Restored sensitivity sliders for Vibration and Tilt in `AlertManagementOverlay` (R247).
*   **Issue #245**: **UI Typo Restoration**: Fixed "Inside Tractor" typo in `strings.xml` (R245).
*   **Issue #243**: **Mode Selection Bypass**: Hardened navigation settling (R243).
*   **Issue #238**: **Anchor Evaluator Coordinate Leak**: Hardened stationary anchor center (R238).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.21.01)
