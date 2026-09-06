# Project Issues & Hardening Tracking (Sep.06.00)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.06.00)
*   **Issue #923 RESOLVED: Lifecycle & Teardown Hardening**. Remediated async races in `HardwareProvider` by joining the teardown window via `teardownJob`. Cleared revival footprint state on session stop to prevent energy leaks and corrected the inverted permission check in GNSS revival pulses.

## 🟢 Recently Resolved Issues (Sep.05.30)
*   **Issue #916 RESOLVED: Energy Footprint Verdict**. Implemented automated mA delta and temperature rise calculation in `HardwareProvider` to quantify the power cost of GNSS revival cycles (R-ID 259).
*   **Issue #921 RESOLVED: Sensor Rate Verification**. Implemented a runtime efficacy audit in `HardwareProvider` for Target SDK 35 (R-ID 256).

## 🟡 Open Issues & Hardening Tasks (Sorted by Criticality)

### 🔴 CRITICAL: Stability & Lifecycle Blockers
*   **Issue #924 (Part A): Watchdog Safe-Mode**.
    *   [ ] **Safe-Mode UI**: When the Hydration Watchdog (#910) fires, the UI must enter a "Safe Mode" that prevents `CommunicationManager` from attempting to connect with null identities, which would cause signaling handshake loops.

### 🟠 HIGH: Forensic Integrity & Signal Parity
*   **Issue #922 (Part A): Clock Parity & Forensic Buffering**.
    *   [ ] **Clock Parity**: Standardize all forensic indexing (SNR, Sensors) in `HardwareProvider` to use monotonic `elapsedRealtime()` as the primary key. Current wall-clock reliance is susceptible to drift during system time jumps (Issue #918 parity).
    *   [ ] **State Buffering**: Replace fixed-array pools with a standardized `CircularStateBuffer` for `ForensicSnapshot` and `snrTsBuffer` to reduce GC pressure and locking overhead on budget devices.

### 🟡 MEDIUM: Refactoring & Budget Optimization
*   **Issue #922 (Part B): HardwareProvider Extraction**.
    *   [ ] **Extraction**: Extract GNSS jitter monitoring, sensor rate audits (R-ID 256), and energy footprint snapshots (R-ID 259) from `HardwareProvider` into a new `ForensicAuditor`. Restore `HardwareProvider` to a lean hardware bridge.
*   **Issue #924 (Part B): A15 Resource Throttling**.
    *   [ ] **Dynamic GNSS Rates**: Move GNSS throttling from the UI layer (`MainViewModel`) down to `HardwareProvider` source. On A15 hardware, throttle emission to 5000ms when high system load or `MaliAnomaly` is detected.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 276 (Rules: 47, IDs: 229), Resolved: 907, Open: 4, Testing: 96% (Chapters), Ideas: 221, QA: 243]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.06.00)*
