# Project Issues & Hardening Tracking (Sep.06.20)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.06.20)
*   **Issue #924 RESOLVED (Part B): A15 Resource Throttling**. Migrated GNSS throttling from the UI layer (`MainViewModel`) down to the `HardwareProvider` source. Implemented resource-aware emission logic that automatically throttles satellite status updates to 5000ms on A15 hardware when high system load or Mali driver anomalies are detected (R-ID 267).

## 🟢 Recently Resolved Issues (Sep.06.17)
*   **Issue #922 RESOLVED (Part B): HardwareProvider Extraction**. Extracted GNSS jitter monitoring, sensor rate audits (R-ID 256), and energy footprint snapshots (R-ID 259) into `ForensicAuditor`. Restored `HardwareProvider` to a lean hardware bridge, adhering to the Single Responsibility Principle.
*   **Issue #922 RESOLVED (Part A): Clock Parity & Forensic Buffering**. Standardized all forensic indexing (SNR, Sensors) in `HardwareProvider` to use monotonic `elapsedRealtime()` as the primary key, ensuring immunity to system time jumps (R-ID 264). Replaced fixed-array pools with a standardized `CircularStateBuffer` for all forensic streams to eliminate GC pressure and locking overhead (R-ID 263).

## 🟢 Recently Resolved Issues (Sep.06.01)
*   **Issue #924 RESOLVED (Part A): Watchdog Safe-Mode**. Implemented a signaling safety barrier (R-ID 271) that engages when the Hydration Watchdog detects a Level 2 hang. The system now enters "Safe Mode," suppressing connection attempts in `CommunicationManager` to prevent handshake loops. Added a "SAF" badge to the HUD for forensic visibility.

## 🟢 Recently Resolved Issues (Sep.06.00)
*   **Issue #923 RESOLVED: Lifecycle & Teardown Hardening**. Remediated async races in `HardwareProvider` by joining the teardown window via `teardownJob`. Cleared revival footprint state on session stop to prevent energy leaks and corrected the inverted permission check in GNSS revival pulses.

## 🟡 Open Issues & Hardening Tasks (Sorted by Criticality)

*(No open issues remaining in this cycle)*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 283 (Rules: 50, IDs: 233), Resolved: 911, Open: 0, Testing: 96% (Chapters), Ideas: 221, QA: 252]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.20)*
