# Project Issues & Hardening Tracking (Aug.10.28)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Action Required | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 573 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #134-Sentinel] [Severity: Low] [Category: Forensic] Silent Failure Detection Latency.**
    *   **Concern**: The current `isSilentFailure` correlation logic is executed within the `IntegrityMonitor` heartbeat (60s interval). This may result in delayed detection of load-correlated stalls if resource spikes are transient or if the stall occurs early in the heartbeat cycle.
    *   **Potential Remediation**: Consider a higher-frequency "Forensic Pulse" (e.g., 10s) for resource-critical correlations.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.10.28)
*   **[Issue #133-Sentinel] [Severity: High] [Category: Forensic] Forensic Anomaly Correlation Engine (Silent Failure Detection).**
    *   **Resolution**: Implemented cross-domain correlation between location stability and hardware resource stress. Added `isSilentFailure` logic to `SentinelValidator` to identify stalls driven by CPU/IO exhaustion (>85% CPU or >800ms IO Latency) rather than physical tamper. Integrated detection into `IntegrityMonitor` and propagated the anomaly flag through forensic ribbons and Protobuf signaling for remote diagnostics. (R133)

---
*   **[Issue #132-Sentinel] [Severity: Medium] [Category: UI] Forensic UI Dashboard Refinement for Performance Metrics.**
    *   **Resolution**: Integrated `cpuLoad`, `ioWait`, and `maxIoLatency` trends into the Tracker and Viewer Forensic Dashboard UI. Refined `ForensicSection` to include a dedicated performance auditing row, enabling visual confirmation of hardware stress and I/O spikes on budget hardware. (R132)

---
*   **[Issue #131-Sentinel] [Severity: High] [Category: Performance] Forensic Performance Audit for budget hardware.**
    *   **Resolution**: Integrated rolling maximum I/O latency tracking into `LatencyMonitor` and `IntegrityMonitor`. Hardened performance auditing for budget hardware (Samsung A15) by triggering forensic alerts upon detecting critical disk spikes (>1000ms). Propagated `maxIoLatency` to the central health state for remote diagnostics. (R131)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.10.28)
