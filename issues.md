# Project Issues & Hardening Tracking (Aug.18.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 646 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #204-C1**: Diagnostic down-sampling reduces forensic fidelity (4Hz vs 100Hz). This is a temporary state for stress-isolation and must be reverted before production release.

---

## 🔴 Open Issues
*   *No active open issues.*

---

## 🟢 Recently Resolved Issues (Aug.18.08)
*   **Issue #204: Diagnostic Stress Isolation (Sensor Sampling Rates)**:
    *   Implemented temporary diagnostic down-sampling to isolate high-frequency overhead (R204).
    *   Reduced `FORENSIC_SAMPLING_INTERVAL_MIN_MS` to 250ms (4Hz) and `MAX` to 500ms (2Hz) in `EngineConstants.kt`.
    *   Down-sampled `AppSensorManager` hardware listeners from `SENSOR_DELAY_FASTEST` to `SENSOR_DELAY_NORMAL` to reduce IMU context-switching stress.
*   **Issue #203: Forensic Multi-Session Alignment Audit (Temporal Hardening)**:
    *   Hardened the forensic telemetry pipeline against temporal jitter and duplication across service restarts (R203).
    *   Refactored `ForensicSpillBuffer` to store absolute `Long` timestamps and `Double` coordinates in the memory-mapped buffer (v3), eliminating session base-time dependencies and overflow risks (R203).
    *   Implemented signature-based deduplication (timestamp + `spillIdx`) in `LogRepository.performForensicDrain` to ensure idempotency during recovery from "dirty" restarts or crashes (R203).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.18.08)
