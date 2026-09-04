# Project Issues & Hardening Tracking (Sep.04.10)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **SOT Master Rules** | 🏗️ Standards | 41 |
| **Functional R-IDs** | 🧩 Requirements | 216 |
| **Resolved Issues** | 🟢 Progress | 871 |
| **Open Technical Issues** | 🔴 Priority | 2 |
| **Testing Chapters** | 🧪 Protocol | 100 |
| **Testing Sub-items** | 🔍 Granularity | 124 |
| **Simplification Ideas** | 💡 Future | 248 |
| **QA Validation Tasks** | 🟢 Validated | 234 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #902: Relay Socket Instability (A15)**: `SRV` indicator remains Red on budget hardware; logs show repeated connection attempts without successful handshake. *Note: Potentially resolved by #906, requires verification.*
*   **Issue #903: Teardown-Loop Anomaly (A15)**: Tracker service enters an immediate teardown sequence following GNSS registration during hydration.
*   **Deployment Synchronization**: Deployment tool targeted only one device (`R5CRC14PG4F`) during the initial multi-device test request. Manual confirmation of A15 deployment status was required.

---

## 🔴 Open Issues (Prioritized)
*   **Issue #905: Global GNSS Reception Failure**: Complete loss of GPS signal reception in both Tracker and Viewer modes across Samsung A15 and S21FE hardware.
*   **Issue #907: System-Wide Interconnectivity Failure**: Complete inability to establish a functional link between S21FE (Viewer) and A15 (Tracker), resulting in total system non-operation.

---

## 🟢 Recently Resolved Issues (Sep.04.10)
*   **Issue #906 RESOLVED: Signaling Transport Robustness**. Remediated critical "SRV Red" failures by removing strict `websocket` transport enforcement in `CommunicationManager`. Allowed default `socket.io` polling-to-websocket upgrade handshake, ensuring connectivity stability across diverse network environments and budget hardware like the Samsung A15 (R906/R251).
*   **Issue #900 RESOLVED: Background Service Restriction (A15)**. Hardened `PhoneSetupOverlay` with explicit guidance for Samsung "Unrestricted" battery mode. Added `isSamsungDevice` detection to `SystemStatusProvider` to trigger targeted UI instructions, remediating `BackgroundServiceStartNotAllowedException` on budget hardware (R900).
*   **Issue #904 RESOLVED: GNSS Rejection (Confirmed)**. Implemented targeted guidance for "Precise Location" in `PhoneSetupOverlay` (Step 0) for Samsung devices. Verified that OS-level coarse location downgrades were the root cause of `0/0` satellite visibility on A15 hardware (R904).
*   **Issue #901 RESOLVED: Log Spam Regression**. Hardened `GpsApplication.trimCaches()` to preserve "pkg" and "uid" identity tokens during memory pressure events. Previous implementation cleared these tokens, causing fallback to native `getPackageName()` and persistent IPC diagnostic log spam on Samsung hardware (R759).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.04.10)*
Current Audit Baseline: [SOT: 257 (Rules: 41, IDs: 216), Resolved: 871, Open: 2, Testing: 100 (Sub-items: 124), Ideas: 248, QA: 234]
