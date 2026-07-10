# System Source of Truth (SoT) Requirements - v9.3.10

This file tracks the authoritative requirements as per project hardening guidelines.

| ID | Requirement | Status |
| :--- | :--- | :--- |
| **R995** | **Signaling Pulse Acknowledgement**: The Tracker MUST explicitly acknowledge Viewer signaling pulses by updating the local remote activity timestamp to ensure HUD visibility and peer activity tracking. | **Verified (v9.3.10)** |
| **R980** | **Peer Activity HUD Authority**: The `GlobalStatusBar` MUST use role-specific freshness logic for peer badges. Trackers MUST bind the "VWR" badge to Viewer pulse activity (local receipt time), while Viewers bind the "TRK" badge to Tracker telemetry freshness. | **Verified (v9.3.8)** |
| **R981** | **Map Marker Stability Authority**: The map system MUST use the `optimizedPoint` from `LocationProcessor` for all remote marker updates. | **Verified (v9.3.8)** |
