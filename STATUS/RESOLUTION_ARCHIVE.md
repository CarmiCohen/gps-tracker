# Resolution Archive (Sep.05.10)

## 🟢 Resolved Issues (Sep.05.10)
*   **Issue #910 RESOLVED: Forensic Stall Simulation (Service Termination Race)**. Remediated a race condition during UI hydration where a transient `null` `appMode` triggered a navigation retreat to the `Landing` route. This activated the `BackHandler` which incorrectly invoked `onCleanupAndExit()`. Hardened `MainAppContent.kt` with a guard preventing Landing navigation if `isSystemActive` is true. Added forensic stack trace logging in `MainActivity.kt` and state transition auditing in `MainViewModel.kt` (R910/R255).

## 🟢 Resolved Issues (Sep.04.40)
*   **Issue #911 RESOLVED: Audit Baseline Synchronization**. Remediated discrepancies in SOT ID counts and Open issue tracking between `issues.md`, `Handover.md`, and `SOT_MASTER_REQUIREMENTS.md`. Established a locked baseline of [SOT: 260, Rules: 41, IDs: 219].
*   **Issue #908 RESOLVED: A15 Lifecycle & Deployment Hardening**. Remediated the "Teardown-Loop Anomaly" on budget hardware by implementing asynchronous, restart-aware thread termination in `HardwareProvider`. Established **R-ID 254** for periodic (60s) signaling identity re-broadcast in `ConnectivitySuite`, ensuring zero-interaction peer discovery during rolling deployments (R908/R254).

## 🟢 Recently Resolved Issues (Sep.04.30)
*   **Issue #907 RESOLVED: System-Wide Interconnectivity Failure**. Remediated critical protocol mismatch by hardening the binary telemetry pipeline. Integrated `SignalingConstants.getTransmissionId()` into `TelemetryProtobufMapper` to ensure ID aliasing consistency (T -> Trk) and implemented role-based packet validation in `CommunicationManager` for binary updates. Restored peer-to-peer handshake functionality between S21FE and A15 (R907/R-ID 253).
*   **Issue #905 RESOLVED: Global GNSS Reception Hardening**. Expanded revival pulse logic in `HardwareProvider` to include `SIGNAL_LOSS` and `GPS_GAP` states. Remediates Samsung A15/S21FE "Zombie GNSS" failure where 0 satellites are reported indefinitely by forcing a hardware-level location update restart (R905/R-ID 252).
*   **Issue #906 RESOLVED: Signaling Transport Robustness**. Remediated critical "SRV Red" failures by removing strict `websocket` transport enforcement in `CommunicationManager`. Allowed default `socket.io` polling-to-websocket upgrade handshake, ensuring connectivity stability across diverse network environments and budget hardware like the Samsung A15 (R906/R251).
