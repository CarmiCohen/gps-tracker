# Project Issues & Hardening Tracking (Aug.18.06)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 644 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No active high-priority risks identified.*

---

## 🔴 Open Issues
*   *No active open issues.*

---

## 🟢 Recently Resolved Issues (Aug.18.06)
*   **Issue #202: Forensic Performance: JNI Memory Pressure Audit**:
    *   Optimized `ForensicSpillBuffer` -> `LogRepository` drainer to eliminate intermediate `LogEntry` allocations (R202).
    *   Verified JNI bridge and `LatencyMonitor` for zero-churn telemetry (R202).
    *   Removed obsolete `peek()` method from `ForensicSpillBuffer` to ensure root-cause remediation and zero-leftovers (R202).
*   **Issue #201: Urban Edge Case: Multipath Mitigation Audit (Core Hardening)**:
    *   Hardened stationary state management against GPS signal bouncing in urban canyons (multipath).
    *   Modified `AnchorEvaluator.kt` to prevent binary anchor release when GPS-derived confidence drops, provided the IMU confirms the device is physically stationary and SNR is low (indicating signal bounce).
    *   Refined `LocationSentinel.kt` to dampen `stationaryProb` decay during low-SNR physically stationary events, preventing jittery state transitions.
    *   Verified `MainAlarmLogic.kt` geofence buffering remains robust during accuracy fluctuations (R201).
*   **Issue #198: Forensic UI Performance & Recomposition Audit**:
    *   Hardened the UI telemetry pipeline by implementing `.sample(100L)` on high-frequency `LocationUpdate` collectors in `MainViewModel.kt`.
    *   Capped UI processing at 10Hz to prevent Main thread saturation during 100Hz forensic bursts while maintaining fluid visual motion.
*   **Issue #197: Forensic Storage-Aware Adaptive Pruning Refinement**:
    *   Hardened storage management for 100Hz sampling by implementing forensic-specific retention limits in `EngineConstants.kt`.
    *   Optimized `LogDao` with chunk-based pruning for `FORENSIC_TRACE` entries to minimize transaction lock duration.
*   **Issue #196: Forensic Log Buffer Pressure Audit**:
    *   Hardened log buffer resilience for 100Hz sampling by increasing `LOG_BUFFER_CAPACITY` to 5000 and `LOG_BATCH_SIZE` to 100.
    *   Refined `LogRepository.kt` drainer logic: lowered `FORENSIC_FILL_THRESHOLD` to 25% to trigger earlier flushing.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.18.06)
