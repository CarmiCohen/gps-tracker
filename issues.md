# Project Issues & Hardening Tracking (Aug.10.30)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Action Required | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 574 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #135] [Severity: High] [Category: Performance] UI Davey/ANR during Overlay Transitions (Settings & Phone Setup).**
    *   **Observed**: Clicking the settings gear icon or the "System Issues" (Phone Setup) button on Samsung A15 triggers an ANR dialog or significant frame skips (>1700ms).
    *   **Concern**: Main thread contention between heavy telemetry flow updates and complex recomposition of large overlays (`SettingsOverlay`, `PhoneSetupOverlay`). The overhead of initializing these components exceeds the available Main-thread budget on budget hardware when telemetry flows are active. (R135)

---

## 🔴 Open Issues
*   **[Issue #135] UI Davey/ANR during Overlay Transitions.** (See details above)

---

## 🟢 Recently Resolved Issues (Aug.10.29)
*   **[Issue #134-Sentinel] [Severity: Medium] [Category: Forensic] Forensic Pulse Frequency Hardening.**
    *   **Resolution**: Implemented a high-frequency "Forensic Pulse" (10s) in `IntegrityMonitor` to reduce the latency of "Silent Failure" detection. Added `FORENSIC_PULSE_INTERVAL_MS` to `EngineConstants.kt`. This ensures that CPU, I/O, and resource-correlated anomalies are audited every 10 seconds, while maintaining the legacy 3-minute threshold for flow stall detection. (R134)

---
*   **[Issue #133-Sentinel] [Severity: High] [Category: Forensic] Forensic Anomaly Correlation Engine (Silent Failure Detection).**
    *   **Resolution**: Implemented cross-domain correlation between location stability and hardware resource stress. Added `isSilentFailure` logic to `SentinelValidator` to identify stalls driven by CPU/IO exhaustion (>85% CPU or >800ms IO Latency) rather than physical tamper. (R133)

---
*   **[Issue #132-Sentinel] [Severity: Medium] [Category: UI] Forensic UI Dashboard Refinement for Performance Metrics.**
    *   **Resolution**: Integrated `cpuLoad`, `ioWait`, and `maxIoLatency` trends into the Tracker and Viewer Forensic Dashboard UI. (R132)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.10.30)
