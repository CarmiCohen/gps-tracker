# Hardening Resolution Archive (Sep.11.35)

## 🟢 Resolved in Sep.11.35
*   **Viewer ID Adoption Failure (#912)**: Corrected a logic error in `TrackerService.handleViewerPulse` where the comparison was made against `DEFAULT_TRACKER_ID` instead of `DEFAULT_VIEWER_ID`. This fix allows the Tracker to correctly adopt and respond to custom Viewer IDs (e.g., `V2`), resolving Red TRK/DAT status on the Viewer.

## 🟢 Resolved in Sep.11.30
*   **A15 Deployment Failure (#908)**: SM-A155F device (`R58X40GV2AR`) successfully detected, deployed, and verified.
*   **Signaling Transport Robustness Verified (#906/R251)**: Verified `polling-to-websocket` fallback configuration.
*   **Protobuf Identity Parity Verified (#907/R253)**: Verified `T -> Trk` aliasing for packet interoperability.

## 🟢 Resolved in Sep.11.23
*   **Signaling Session Integrity (#313/R-ID 313)**: Hardened `CommunicationManager` with session-ID isolation.
*   **Silent Failure Correlation (#133/R-ID 312)**: Corrected `isTamperDetected` propagation in alarm logic.

---
*For older records, see historical git logs. (vSep.11.35)*
