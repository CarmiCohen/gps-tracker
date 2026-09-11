# Project Issues & Hardening Tracking (Sep.11.23)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and Silent Failure correlation have been fully remediated and audited. Focus shifts to final verification of hardware LEDs, signaling stability, and telemetry backfill convergence in the Sep.11.23 release candidate.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **A15 Deployment Failure (#908)**: The SM-A155F device was not detected during the Sep.11.23 deployment cycle. Physical testing for Chapters 5.1, 16.1, and 22.1 is blocked until both target devices are active.

## 🟢 Recently Resolved Issues (Sep.11.23)
*   **GNSS Zombie Recovery Logic Verified (#905/R252)**:
    *   **Root-Cause Remediation**: Verified `HardwareProvider` revival pulse logic (30s intervals) and hardware-level reset triggers.
*   **Signaling Transport Robustness Verified (#906/R251)**:
    *   **Root-Cause Remediation**: Verified `polling-to-websocket` fallback configuration in `CommunicationManager`.
*   **Protobuf Identity Parity Verified (#907/R253)**:
    *   **Root-Cause Remediation**: Verified `T -> Trk` aliasing in `TelemetryProtobufMapper` for binary packet interoperability.
*   **Signaling Session Integrity RESOLVED (#313/R-ID 313)**:
    *   **Root-Cause Remediation**: Remediated signaling race conditions during rapid role transitions on high-latency networks.
*   **Silent Failure Correlation Hardening RESOLVED (#133/R-ID 312)**:
    *   **Root-Cause Remediation**: Corrected a plumbing gap where `isTamperDetected` was not correctly propagated into the alarm evaluation state.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 992, Open: 1, Testing: 100% (Sub-items: 51), Ideas: 7, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.23)*
