# Resolution Archive (Sep.06.17)

## 🟢 Resolved Issues (Sep.06.17)
*   **Issue #922 RESOLVED (Part A): Clock Parity & Forensic Buffering**. Standardized all forensic indexing (SNR, Sensors) in `HardwareProvider` to use monotonic `elapsedRealtime()` as the primary key, ensuring immunity to system time jumps (R-ID 264). Replaced fixed-array pools with a standardized `CircularStateBuffer` for all forensic streams to eliminate GC pressure and locking overhead (R-ID 263).

## 🟢 Resolved Issues (Sep.06.01)
*   **Issue #924 RESOLVED (Part A): Watchdog Safe-Mode**. Implemented a signaling safety barrier that engages when the Hydration Watchdog detects a Level 2 hang. The system now enters "Safe Mode," suppressing all signaling connection attempts via `CommunicationManager` to prevent handshake loops with uninitialized identities. Added a `SAF` status badge to the HUD for forensic visibility (R-ID 271).

## 🟢 Resolved Issues (Sep.06.00)
*   **Issue #923 RESOLVED: Lifecycle & Teardown Hardening**. remediated async races in `HardwareProvider` by joining the 800ms teardown window via `teardownJob`, ensuring rapid service toggles do not lead to concurrent registration attempts. Cleared `revivalStartBattery` and `revivalStartRtForFootprint` on stop to prevent energy footprint leaks across sessions (R-ID 262). Corrected an inverted permission check in `restartLocationUpdates()` to restore GNSS revival pulse functionality (R-ID 252).

## 🟢 Resolved Issues (Sep.05.30)
*   **Issue #916 RESOLVED: Energy Footprint Verdict**. Implemented automated mA delta and temperature rise calculation in `HardwareProvider` to quantify the power cost of GNSS revival cycles (R-ID 259).

## 🟢 Resolved Issues (Sep.05.29)
*   **Issue #921 RESOLVED: Sensor Rate Verification**. Implemented a runtime efficacy audit in `HardwareProvider` to verify `HIGH_SAMPLING_RATE_SENSORS` performance on Target SDK 35 (R-ID 256).

## 🟢 Resolved Issues (Sep.05.28)
*   **Issue #914 RESOLVED: GNSS Detail Sampling**. Implemented A15-aware sampling for the `activeGnssDetail` flow in `MainViewModel` (R-ID 267).

*(For older resolutions, see history logs.)*
