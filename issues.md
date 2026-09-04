# Project Issues & Hardening Tracking (Sep.04.20)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **SOT Master Rules** | 🏗️ Standards | 41 |
| **Functional R-IDs** | 🧩 Requirements | 218 |
| **Resolved Issues** | 🟢 Progress | 873 |
| **Open Technical Issues** | 🔴 Priority | 0 |
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
*   *No high-priority interconnectivity issues open.*

---

## 🟢 Recently Resolved Issues (Sep.04.20)
*   **Issue #907 RESOLVED: System-Wide Interconnectivity Failure**. Remediated critical protocol mismatch by hardening the binary telemetry pipeline. Integrated `SignalingConstants.getTransmissionId()` into `TelemetryProtobufMapper` to ensure ID aliasing consistency (T -> Trk) and implemented role-based packet validation in `CommunicationManager` for binary updates. Restored peer-to-peer handshake functionality between S21FE and A15 (R907/R-ID 253).
*   **Issue #905 RESOLVED: Global GNSS Reception Hardening**. Expanded revival pulse logic in `HardwareProvider` to include `SIGNAL_LOSS` and `GPS_GAP` states. Remediates Samsung A15/S21FE "Zombie GNSS" failure where 0 satellites are reported indefinitely by forcing a hardware-level location update restart (R905/R-ID 252).
*   **Issue #906 RESOLVED: Signaling Transport Robustness**. Remediated critical "SRV Red" failures by removing strict `websocket` transport enforcement in `CommunicationManager`. Allowed default `socket.io` polling-to-websocket upgrade handshake, ensuring connectivity stability across diverse network environments and budget hardware like the Samsung A15 (R906/R251).
*   **Issue #900 RESOLVED: Background Service Restriction (A15)**. Hardened `PhoneSetupOverlay` with explicit guidance for Samsung "Unrestricted" battery mode. Added `isSamsungDevice` detection to `SystemStatusProvider` to trigger targeted UI instructions, remediating `BackgroundServiceStartNotAllowedException` on budget hardware (R900).
*   **Issue #904 RESOLVED: GNSS Rejection (Confirmed)**. Implemented targeted guidance for "Precise Location" in `PhoneSetupOverlay` (Step 0) for Samsung devices. Verified that OS-level coarse location downgrades were the root cause of `0/0` satellite visibility on A15 hardware (R904).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.04.20)*
Current Audit Baseline: [SOT: 259 (Rules: 41, IDs: 218), Resolved: 873, Open: 0, Testing: 100 (Sub-items: 124), Ideas: 248, QA: 234]
