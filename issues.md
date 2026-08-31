# Project Issues & Hardening Tracking (Aug.31.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 28 |
| **Validation Tasks** | 🟢 Validated | 208 |
| **Resolved (Total)** | 🟢 Progress | 784 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None identified in current audit cycle)*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.31.00)
*   **Issue #782 Resolved: Protocol Audit - Binary Schema Expansion & Forensic Audit**. Expanded `RealtimeStatus` Protobuf schema to carry `violationUptimeMs` and `isUltraLongStationary`. Hardened `TelemetryMapper` and `ConnectionPoint` to ensure full forensic parity in history persistence and UI Replay.
*   **Issue #779: Forensic Metadata Leak Cleanup**. Implemented `ForensicSanitizer` to scrub absolute internal paths and normalize hardware identifiers in all exported logs and telemetry.
*   **Concern #781 Resolved: Documentation Integrity Restoration**. Restored exhaustive Functional Requirements (R101-R999) to `SOT_MASTER_REQUIREMENTS.md`.
*   **Bug Fix**: Resolved a logic error in `TrackerStatus.toMap()` where `baro_idx` was incorrectly reassigned instead of mapped.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.31.00)*
*Simplification Ideas: 216*
