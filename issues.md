# Project Issues & Hardening Tracking (July.27.06)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 438 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #602] [Severity: Low] [Category: Sensors] SIT Timestamp Parity Logic**. 
    - **Risk**: While SIT timestamps (sitVzTs, sitVzRt) were restored in this cycle, the forensic playback engine may require an audit to ensure these are correctly visualized in "Strict Mode".
    - **Trigger**: Opening historical SIT events in the Analytical Ribbon UI.

---

## 🔴 Open Issues
*   *(No active critical engine issues)*

---

## 🟢 Recently Resolved Issues (July.27.06)
*   **[Issue #601] [Severity: Med] [Category: Sensors] Kinetic Energy Anomaly Detection**.
    - **Resolution**: Implemented a centralized High-Pass Filter (HPF) and Energy EMA in `SentinelValidator` to isolate impulsive shocks from sustained motion. Integrated `kineticEnergy` across sensor management, engine validation, and telemetry layers to improve SIT/STAND detection reliability.
    - **Validation**: Verified that high-G impulse events do not trigger sustained motion states. `:app:assembleDebug` success.

*   **[Issue #118.1] [Severity: Low] [Category: Arch] Forensic Timestamp Parity**.
    - **Resolution**: Restored missing `sitVzTs` and `sitVzRt` fields in `LocationUpdate`, `TrackerStatus`, and Protobuf schema to ensure architectural parity (R118) between engine and persistence.
    - **Validation**: Verified binary payload continuity in `ConnectivitySuite`.

---

## 🟢 Recently Resolved Issues (July.27.05)
*   **[Issue #600] [Severity: Med] [Category: I/O] Forensic Playback Latency Audit**.
*   **[Issue #600.1] [Severity: Low] [Category: Arch] Metadata Standardization**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
