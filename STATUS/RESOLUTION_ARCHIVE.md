# Resolution Archive (Sep.06.30)

## 🟢 Resolved Issues (Sep.06.30)
*   **Issue #925 RESOLVED: Async Teardown Race Condition**. Remediated critical race condition where rapid `stop() -> start()` sequences attempted re-initialization before the forensic settling window (800ms) completed. Converted `HardwareProvider.start()` to a suspend function that joins the `teardownJob` for deterministic lifecycle transitions (R925).

## 🟢 Resolved Issues (Sep.06.20)
*   **Issue #924 RESOLVED (Part B): A15 Resource Throttling**. Migrated GNSS throttling from the UI layer (`MainViewModel`) down to the `HardwareProvider` source. Implemented resource-aware emission logic that automatically throttles satellite status updates to 5000ms on A15 hardware when high system load or Mali driver anomalies are detected (R-ID 267).

## 🟢 Resolved Issues (Sep.06.17)
*   **Issue #922 RESOLVED (Part B): HardwareProvider Extraction**. Extracted GNSS jitter monitoring, sensor rate audits (R-ID 256), and energy footprint snapshots (R-ID 259) into `ForensicAuditor`. Restored `HardwareProvider` to a lean hardware bridge, adhering to the Single Responsibility Principle.
*   **Issue #922 RESOLVED (Part A): Clock Parity & Forensic Buffering**. Standardized all forensic indexing (SNR, Sensors) in `HardwareProvider` to use monotonic `elapsedRealtime()` as the primary key, ensuring immunity to system time jumps (R-ID 264). Replaced fixed-array pools with a standardized `CircularStateBuffer` for all forensic streams to eliminate GC pressure and locking overhead (R-ID 263).

## 🟢 Resolved Issues (Sep.06.01)
*   **Issue #924 RESOLVED (Part A): Watchdog Safe-Mode**. Implemented a signaling safety barrier that engages when the Hydration Watchdog detects a Level 2 hang. The system now enters "Safe Mode," suppressing all signaling connection attempts via `CommunicationManager` to prevent handshake loops with uninitialized identities. Added a `SAF` status badge to the HUD for forensic visibility (R-ID 271).

## 🟢 Resolved Issues (Sep.06.00)
*   **Issue #923 RESOLVED: Lifecycle & Teardown Hardening**. remediated async races in `HardwareProvider` by joining the 800ms teardown window via `teardownJob`, ensuring rapid service toggles do not lead to concurrent registration attempts. Cleared `revivalStartBattery` and `revivalStartRtForFootprint` on stop to prevent energy footprint leaks across sessions (R-ID 262). Corrected an inverted permission check in `restartLocationUpdates()` to restore GNSS revival pulse functionality (R-ID 252).

*(For older resolutions, see history logs.)*
