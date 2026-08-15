# Project Issues & Hardening Tracking (Aug.14.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 1 | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 613 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #173] [Severity: High] [Category: Architecture] Multi-Stream Processor Contention.**
    *   **Concern**: `ViewerService` uses a single `LocationProcessor` instance to filter both the viewer's own GPS points and the remote tracker's telemetry. Since `LocationProcessor` maintains internal monotonic state (last position, velocity EMA, filter buffers), interleaving these two distinct spatial streams causes filter corruption, resulting in inaccurate jump detection and UI ribbon instability on the viewer side.
    *   **Mitigation**: `ViewerService` must instantiate two separate `LocationProcessor` instances: one for the "Self" stream and one for the "Remote" stream.

---

## 🔴 Open Issues
*   **[Issue #172] [Severity: High] [Category: Data Integrity] Viewer-Side LocationProcessor State Audit.**
    *   **Goal**: Ensure `LocationProcessor` on the viewer side correctly restores forensic state (SIT, Tilt, MaxAccuracy) from remote telemetry after service restarts.

---

## 🟢 Recently Resolved Issues (Aug.14.03)
*   **[Issue #171] [Severity: High] [Category: Performance/Data Integrity] Forensic Multi-Stream Jitter Audit.**
    *   **Resolution**: Hardened the forensic telemetry pipeline against non-monotonic packet arrival (jitter) caused by multi-viewer streams or network delays. Relaxed `RemoteStatusRepository` to allow a 2s jitter window (`MONOTONIC_JITTER_TOLERANCE_MS`) to prevent forensic data loss. Implemented a monotonicity guard in `TelemetryAggregator` to ensure aggregators don't regress. Hardened `StateSubscriptionUseCase` to perform sorted-merging and deduplication of history buffers for stable UI ribbon visualization. Verified via artificial jitter simulation (200-800ms) in `CommunicationManager`. (R171)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.14.04)
