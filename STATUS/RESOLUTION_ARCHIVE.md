# Resolution Archive (Sep.14.52)

## 🟢 Sep.14.52
*   **Signaling State Reduction (#1041)**: Simplified the sealed class hierarchy in `CommandRouter` by removing redundant `ViewerPulse` and `TransientDrop` events. Unified peer vitality detection around `ConnectivityEvent.PeerPulse` and removed legacy `ACTION_RELAY_STATUS` broadcast leftovers. (R-ID 335).

## 🟢 Sep.14.50
*   **Redundant Logic Pruning (#1040)**: Pruned legacy backfill triggers in `ConnectivitySuite` handled by the 60s identity sync loop. (R-ID 334).

## 🟢 Sep.14.47
*   **Signaling Forensic Decoupling (#1039)**: Migrated signaling drop logging to `SignalingForensicLogger`. (R-ID 333).
*   **A15 Compliance (#1038)**: Implemented 10s forensic log throttling. (R-ID 332).
