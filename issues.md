# Project Issues & Hardening Tracking (Sep.04.40)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **SOT Master Rules** | 🏗️ Standards | 41 |
| **Functional R-IDs** | 🧩 Requirements | 219 |
| **Resolved Issues** | 🟢 Progress | 874 |
| **Open Technical Issues** | 🔴 Priority | 0 |
| **Testing Chapters** | 🧪 Protocol | 100 |
| **Testing Sub-items** | 🔍 Granularity | 124 |
| **Simplification Ideas** | 💡 Future | 250 |
| **QA Validation Tasks** | 🟢 Validated | 240 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No high-priority technical risks identified.*

---

## 🔴 Open Issues (Prioritized)
*   *No high-priority interconnectivity issues open.*

---

## 🟢 Recently Resolved Issues (Sep.04.40)
*   **Issue #908 RESOLVED: A15 Lifecycle & Deployment Hardening**. Remediated the "Teardown-Loop Anomaly" on budget hardware by implementing asynchronous, restart-aware thread termination in `HardwareProvider`. Established **R-ID 254** for periodic (60s) signaling identity re-broadcast in `ConnectivitySuite`, ensuring zero-interaction peer discovery during rolling deployments (R908/R254).
*   **Issue #907 RESOLVED: System-Wide Interconnectivity Failure**. Remediated critical protocol mismatch by hardening the binary telemetry pipeline (R907/R-ID 253).
*   **Issue #905 RESOLVED: Global GNSS Reception Hardening**. Expanded revival pulse logic for SIGNAL_LOSS and GPS_GAP states (R905/R-ID 252).
*   **Issue #906 RESOLVED: Signaling Transport Robustness**. Remediated "SRV Red" failures by allowing default socket.io transport negotiation (R906/R251).
*   **Issue #900 RESOLVED: Background Service Restriction (A15)**. Hardened `PhoneSetupOverlay` with Samsung-specific battery guidance (R900).
*   **Issue #904 RESOLVED: GNSS Rejection (Confirmed)**. Implemented targeted guidance for "Precise Location" in UI (R904).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.04.40)*
Current Audit Baseline: [SOT: 260 (Rules: 41, IDs: 219), Resolved: 874, Open: 0, Testing: 100 (Sub-items: 124), Ideas: 250, QA: 240]
