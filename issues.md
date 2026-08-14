# Project Issues & Hardening Tracking (Aug.14.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 0 | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 613 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *None at this time.*

---

## 🔴 Open Issues
*   *No critical open issues.*

---

## 🟢 Recently Resolved Issues (Aug.14.03)
*   **[Issue #171] [Severity: High] [Category: Performance/Data Integrity] Forensic Multi-Stream Jitter Audit.**
    *   **Resolution**: Hardened the forensic telemetry pipeline against non-monotonic packet arrival (jitter) caused by multi-viewer streams or network delays. Relaxed `RemoteStatusRepository` to allow a 2s jitter window (`MONOTONIC_JITTER_TOLERANCE_MS`) to prevent forensic data loss. Implemented a monotonicity guard in `TelemetryAggregator` to ensure aggregators don't regress. Hardened `StateSubscriptionUseCase` to perform sorted-merging and deduplication of history buffers for stable UI ribbon visualization. Verified via artificial jitter simulation (200-800ms) in `CommunicationManager`. (R171)

---

## 🟢 Recently Resolved Issues (Aug.14.02)
*   **[Issue #170] [Severity: Medium] [Category: UI/UX] Forensic Replay UI Audit.**
    *   **Resolution**: Restored coordinate-aware scrubbing in `AnalyticalRibbons`. Implemented `replayCursorTs` synchronization between ribbons and map. Utilized binary search for frame-perfect coordinate matching during high-frequency (100Hz) replay simulation. Verified zero-drift alignment between `vibeIdx` spikes and map marker positioning. (R170)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.14.03)
