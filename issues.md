# Project Issues & Hardening Tracking (Aug.10.31)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 575 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #136] [Severity: Low] [Category: Performance] Compose Preview Coverage Gap.**
    *   **Concern**: The recent decomposition of overlays significantly changed their signatures. Compose Previews for `SettingsOverlay` and `PhoneSetupOverlay` need to be updated to ensure visual regression testing remains viable. (R136)

---

## 🔴 Open Issues
*   *No high-severity open issues.*

---

## 🟢 Recently Resolved Issues (Aug.10.31)
*   **[Issue #135] [Severity: High] [Category: Performance] UI Davey/ANR Mitigation for Overlay Transitions.**
    *   **Resolution**: Refactored `SettingsOverlay`, `PhoneSetupOverlay`, and `DiagnosticsScreen` to use fully decomposed primitive parameters instead of monolithic `MainUiState` and `DiagnosticState`. This isolates complex UI components from high-frequency telemetry recomposition triggers (GPS/Sensor updates), eliminating 1700ms+ main-thread stalls and ANR risks on budget hardware (Samsung A15). (R135)

---

## 🟢 Recently Resolved Issues (Aug.10.29)
*   **[Issue #134-Sentinel] [Severity: Medium] [Category: Forensic] Forensic Pulse Frequency Hardening.**
    *   **Resolution**: Implemented a high-frequency "Forensic Pulse" (10s) in `IntegrityMonitor` to reduce the latency of "Silent Failure" detection. Added `FORENSIC_PULSE_INTERVAL_MS` to `EngineConstants.kt`. (R134)

---
*   **[Issue #133-Sentinel] [Severity: High] [Category: Forensic] Forensic Anomaly Correlation Engine (Silent Failure Detection).**
    *   **Resolution**: Implemented cross-domain correlation between location stability and hardware resource stress. (R133)

---
*   **[Issue #132-Sentinel] [Severity: Medium] [Category: UI] Forensic UI Dashboard Refinement for Performance Metrics.**
    *   **Resolution**: Integrated `cpuLoad`, `ioWait`, and `maxIoLatency` trends into the Tracker and Viewer Forensic Dashboard UI. (R132)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.10.31)
