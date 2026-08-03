# Project Issues & Hardening Tracking (Aug.03.55)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 513 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (Aug.03.55)
*   **[Issue #704] [Severity: Medium] [Category: Performance] Forensic Audit: Trace Backfill Flow Hardening**.
    *   **Resolution**: Hardened the forensic backfill process by implementing a transactional peek/commit pattern in `ForensicSpillBuffer`. Added `readIdx` to the persistent header to track consumption independently of production. Refactored `LogRepository` to perform forensic database insertions outside the global mutex, eliminating I/O contention and ensuring that real-time log flushing remains responsive during high-volume forensic bursts (R704).
*   **[Issue #703] [Severity: Medium] [Category: Robustness] Forensic Audit: Trace Recovery Integrity Validation**.
    *   **Resolution**: Hardened `ForensicSpillBuffer` recovery logic. Added a `MAGIC_NUMBER` to the header and implemented recovery-time sanity checks for circular buffer indices. Added CRC32 checksum validation for each individual trace entry to detect data corruption after system crashes (R703).
*   **[Issue #702] [Severity: Medium] [Category: Performance] Forensic Audit: Trace Serialization Hardening**.
    *   **Resolution**: Implemented full binary serialization for the forensic hot-path. Raw telemetry (battery level, charging status, temperature) is now serialized directly to `MappedByteBuffer` as primitive types (R702).
*   **[Issue #701] [Severity: Medium] [Category: Performance] Forensic Audit: Spatial Quantization for Trace Compression**.
    *   **Resolution**: Implemented spatial quantization in the forensic logging loop. Traces are now suppressed if displacement is less than 0.1m, unless IMU sensors detect significant vibration or tilt (R701).
*   **[Issue #700] [Severity: Medium] [Category: Performance] Forensic Audit: Power-Aware Sampling Scaling**.
    *   **Resolution**: Implemented dynamic forensic sampling rates (10Hz - 100Hz) based on power and thermal state (R700).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.03.55)
