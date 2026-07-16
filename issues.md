# Project Issues & Hardening Tracking (July.16.18)

This document tracks active issues, technical debt, and pending implementation tasks.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 289 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **ConnectivitySuite Scope**: Consolidates network, sync, and remote handling. Maintain internal modularity to prevent bloat.
*   **AppContainer Circularity**: Resolved via lambda/lazy. Must be preserved in future DI changes.
*   **Reduced SNR Visibility**: The removal of SNR sampling (Issue #514) reduces forensic signal-density data in ribbons. This is an intentional trade-off for reduced CPU/Memory overhead.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (July.16.18)
*   **Issue #514 (R406i)**: Simplify GpsManager.
    *   Removed `kickGps` and `reviveGps` legacy commands.
    *   Removed `snrBuffer` and `getSnrSamples`.
    *   Simplified `GnssStatus.Callback` to provide only immediate metadata.
    *   Refactored `HistoryManager` and `TelemetryAggregator` to remove SNR sampling dependencies.
    *   Purged `EngineSnrSample` from models.

*   **Issue #513 (R406h)**: Flatten Service Architecture (ConnectivitySuite).
    *   Merged `AppNetworkManager`, `SyncManager`, and `RemoteHandler` into `ConnectivitySuite`.
    *   Removed redundant `RemoteUpdateWrapper`.
    *   Streamlined service dependency graph.
