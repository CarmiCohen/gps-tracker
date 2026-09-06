# Project Issues & Hardening Tracking (Sep.06.17)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.06.17)
*   **Issue #922 RESOLVED (Part A): Clock Parity & Forensic Buffering**. Standardized all forensic indexing (SNR, Sensors) in `HardwareProvider` to use monotonic `elapsedRealtime()` as the primary key, ensuring immunity to system time jumps (R-ID 264). Replaced fixed-array pools with a standardized `CircularStateBuffer` for all forensic streams to eliminate GC pressure and locking overhead (R-ID 263).

## 🟢 Recently Resolved Issues (Sep.06.01)
*   **Issue #924 RESOLVED (Part A): Watchdog Safe-Mode**. Implemented a signaling safety barrier (R-ID 271) that engages when the Hydration Watchdog detects a Level 2 hang. The system now enters "Safe Mode," suppressing connection attempts in `CommunicationManager` to prevent handshake loops. Added a "SAF" badge to the HUD for forensic visibility.

## 🟢 Recently Resolved Issues (Sep.06.00)
*   **Issue #923 RESOLVED: Lifecycle & Teardown Hardening**. Remediated async races in `HardwareProvider` by joining the teardown window via `teardownJob`. Cleared revival footprint state on session stop to prevent energy leaks and corrected the inverted permission check in GNSS revival pulses.

## 🟡 Open Issues & Hardening Tasks (Sorted by Criticality)

### 🟡 MEDIUM: Refactoring & Budget Optimization
*   **Issue #922 (Part B): HardwareProvider Extraction**.
    *   [ ] **Extraction**: Extract GNSS jitter monitoring, sensor rate audits (R-ID 256), and energy footprint snapshots (R-ID 259) from `HardwareProvider` into a new `ForensicAuditor`. Restore `HardwareProvider` to a lean hardware bridge.
*   **Issue #924 (Part B): A15 Resource Throttling**.
    *   [ ] **Dynamic GNSS Rates**: Move GNSS throttling from the UI layer (`MainViewModel`) down to `HardwareProvider` source. On A15 hardware, throttle emission to 5000ms when high system load or `MaliAnomaly` is detected.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 281 (Rules: 49, IDs: 232), Resolved: 909, Open: 2, Testing: 96% (Chapters), Ideas: 221, QA: 252]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.17)*
