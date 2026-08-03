# Project Issues & Hardening Tracking (Aug.03.50)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 512 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (Aug.03.50)
*   **[Issue #703] [Severity: Medium] [Category: Robustness] Forensic Audit: Trace Recovery Integrity Validation**.
    *   **Resolution**: Hardened `ForensicSpillBuffer` recovery logic. Added a `MAGIC_NUMBER` to the header and implemented recovery-time sanity checks for circular buffer indices. Added CRC32 checksum validation for each individual trace entry to detect data corruption after system crashes. Refactored checksum calculation to use a reusable `ByteArray` for API 24 compatibility without sacrificing zero-allocation hot-path performance (R703).
*   **[Issue #702] [Severity: Medium] [Category: Performance] Forensic Audit: Trace Serialization Hardening**.
    *   **Resolution**: Implemented full binary serialization for the forensic hot-path. Raw telemetry (battery level, charging status, temperature) is now serialized directly to `MappedByteBuffer` as primitive types. Human-readable message formatting is deferred to the background drainer, eliminating string allocation overhead in the 100Hz sampling loop (R702).
*   **[Issue #701] [Severity: Medium] [Category: Performance] Forensic Audit: Spatial Quantization for Trace Compression**.
    *   **Resolution**: Implemented spatial quantization in the forensic logging loop. Traces are now suppressed if displacement is less than 0.1m, unless IMU sensors detect significant vibration (>0.05G) or tilt (>1.0°). This significantly reduces storage churn and I/O overhead during stationary periods without compromising forensic fidelity for critical events (R701).
*   **[Issue #700] [Severity: Medium] [Category: Performance] Forensic Audit: Power-Aware Sampling Scaling**.
    *   **Resolution**: Implemented dynamic forensic sampling rates (10Hz - 100Hz) based on `isCharging` and `isCoolingModeActive`. Integrated zero-allocation logging path (`logForensicTraceOptimized`) to ensure R668 compliance during 100Hz capture, optimizing battery consumption while maintaining forensic integrity (R700).
*   **[Issue #669] [Severity: High] [Category: Performance] Forensic Audit: Database I/O Contention under High-Frequency Trace Bursts**.
    *   **Resolution**: Implemented `ForensicSpillBuffer` using `MappedByteBuffer` to decouple trace capture from SQLite persistence (R-HARDWARE-01).
*   **[Issue #668] [Severity: Medium] [Category: Performance] Forensic Audit: Object Churn in Telemetry Pipelines**.
    *   **Resolution**: Implemented zero-allocation telemetry path using mutable flyweight patterns (R668).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.03.50)
