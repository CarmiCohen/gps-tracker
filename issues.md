# Project Issues & Hardening Tracking (Sep.04.40)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **SOT Master Rules** | 🏗️ Standards | 41 |
| **Functional R-IDs** | 🧩 Requirements | 220 |
| **Resolved Issues** | 🟢 Progress | 877 |
| **Open Technical Issues** | 🔴 Priority | 1 |
| **Testing Chapters** | 🧪 Protocol | 100 |
| **Testing Sub-items** | 🔍 Granularity | 127 |
| **Simplification Ideas** | 💡 Future | 250 |
| **QA Validation Tasks** | 🟢 Validated | 243 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #909: Device Detection Anomaly**. Deployment tool fails to resolve the **Samsung A15** target, repeatedly defaulting to the S21FE (`R5CRC14PG4F`). This prevents live verification of A15-specific hardware regressions (e.g., #908 settling window).

---

## 🔴 Open Issues (Prioritized)
*   **Issue #909**: Investigate ADB/Deployment tool targeting for multi-device environments.

---

## 🟢 Recently Resolved Issues (Sep.04.40)
*   **Partial Test (5.1, 16.1, 22.1) VERIFIED**: Logic and log signatures for GNSS Revival, Transport Robustness (Polling fallback), and Protobuf Identity Parity (T -> Trk) verified on S21FE/Code Audit.
*   **Issue #908 RESOLVED: A15 Lifecycle & Deployment Hardening**. Remediated the "Teardown-Loop Anomaly" on budget hardware by implementing asynchronous, restart-aware thread termination in `HardwareProvider`. Established **R-ID 254** for periodic (60s) signaling identity re-broadcast in `ConnectivitySuite`.
*   **Issue #907 RESOLVED: System-Wide Interconnectivity Failure**. Remediated critical protocol mismatch by hardening the binary telemetry pipeline (R907/R-ID 253).
*   **Issue #905 RESOLVED: Global GNSS Reception Hardening**. Expanded revival pulse logic for SIGNAL_LOSS and GPS_GAP states (R905/R-ID 252).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.04.40)*
Current Audit Baseline: [SOT: 261 (Rules: 41, IDs: 220), Resolved: 877, Open: 1, Testing: 100 (Sub-items: 127), Ideas: 250, QA: 243]
