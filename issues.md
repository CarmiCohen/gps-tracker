# Project Issues & Hardening Tracking (Sep.11.30)

## 🎯 Current Resumption Focus: Release Candidate Deployment & LED Verification
Signaling session integrity and Silent Failure correlation have been fully remediated and audited. Focus shifts to final verification of hardware LEDs, signaling stability, and telemetry backfill convergence in the Sep.11.30 release candidate.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   *(None currently identified)*

## 🟢 Recently Resolved Issues (Sep.11.30)
*   **A15 Deployment Failure RESOLVED (#908)**: 
    *   **Root-Cause Remediation**: SM-A155F device (`R58X40GV2AR`) successfully detected, deployed, and verified with Sep.11.30 build. Connectivity and hardware initialization confirmed via logcat and UI audit.
*   **GNSS Zombie Recovery Logic Verified (#905/R252)**:
    *   **Root-Cause Remediation**: Verified `HardwareProvider` revival pulse logic (30s intervals) and hardware-level reset triggers.
*   **Signaling Transport Robustness Verified (#906/R251)**:
    *   **Root-Cause Remediation**: Verified `polling-to-websocket` fallback configuration in `CommunicationManager`.
*   **Protobuf Identity Parity Verified (#907/R253)**:
    *   **Root-Cause Remediation**: Verified `T -> Trk` aliasing in `TelemetryProtobufMapper` for binary packet interoperability.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 314 (Rules: 58, IDs: 256), Resolved: 993, Open: 0, Testing: 100% (Sub-items: 51), Ideas: 7, QA: 270]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.11.30)*
