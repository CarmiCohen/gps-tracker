# Forensic Handover - v9.1.1 (Identity Locking Refinement)

## 📌 Status: Stable / Build PASS / Identity Lock Refined
This cycle refines R982: Identity Locking to support first-time pairing while maintaining peer-to-peer security.

### 🟢 Completed: Requirement R982 Refinement (Lock-on-Non-Default)
*   **Signaling Guard**: 
    *   Updated `SignalingValidator` in `:core:engine` to allow packets if the tracker is in the "Default/Unlocked" state (Viewer ID = "V").
    *   Once a non-default `viewerId` is adopted, the tracker transitions to a "Locked" state, exclusively processing packets from that ID.
*   **Communication Hardening**:
    *   Applied "Default Relaxation" to `CommunicationManager` pulse and ping handlers.
    *   Ensured trackers can still "adopt" the first valid viewer pulse they receive.
*   **Documentation**:
    *   Updated `requirements_sot.md` to reflect the refined R982 behavioral authority.
    *   Resolved the pairing regression identified during v9.1.0 testing.

### 🛠 Instructions for Resumption
1.  **Pairing Verification**: Perform a fresh install and verify the Tracker correctly adopts a custom Viewer ID on the first pulse.
2.  **Lock Verification**: Once paired, verify the Tracker ignores a third-party Viewer attempting to send commands with a different ID.
3.  **Audit**: Review `CommunicationManager` for any remaining legacy ID checks that bypass the `SignalingValidator`.
