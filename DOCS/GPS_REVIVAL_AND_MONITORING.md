# GPS Revival & Advanced Monitoring Mechanisms (v8.8.35)

This document explains the specialized monitoring logic used to detect "silent" failures like GPS stalling, signal jamming, and the mechanisms used to "revive" data flow.

## 1. GPS Stalling Detection & Escalated Revival
**Problem**: Sometimes the GPS hardware remains "active" but stops providing new coordinates, or provides old "stale" data repeatedly.

**Mechanism**:
- **Stall Watchdog**: The `LocationProcessor` tracks the raw hardware timestamp. If the coordinates are identical across updates while vibration sensors indicate movement, it flags a stall.
- **UI Threshold (7s)**: The UI uses `GPS_UI_FAIL_THRESHOLD_MS` (7 seconds) as a strict "Position Health" gate. 
- **Instant Recovery (R923)**: Freshness logic utilizes the maximum of the GPS timestamp and the telemetry arrival timestamp (`telemetryTs`) to prevent dashboard "gray-out" during link restoration.
- **Escalated Revival (Issue 124)**: Sustained stalls beyond `GPS_STALL_THRESHOLD_MS` (180s) trigger a hardware-level refresh cycle:
    - **Retry Loop**: The system attempts to revive the GPS hardware every 5 minutes (`GPS_REVIVAL_RETRY_INTERVAL_MS`).
    - **Max Attempts**: After 3 failed attempts (`MAX_REVIVAL_ATTEMPTS`), the system escalates to a **CRITICAL forensic alert**.
    - **Viewer Notification**: A `CRITICAL: GPS_HARDWARE_LOCK` log is emitted to signal that manual intervention or physical relocation is required.
    - **Auto-Reset**: The revival counter is reset immediately upon receiving a valid, fresh coordinate fix.

## 2. Jammer Detection & Hardening (v8.8.35)
**Problem**: Sophisticated jammers might block GPS signals while leaving data connections active.

**Mechanism**:
- **Cross-Validation**: The system monitors the relay connection independently of the GNSS fix.
- **Suspicion Trigger**: If the device is connected to the relay but hasn't received a fresh GPS update while high vibration indicates movement, it triggers `isJammerSuspicion`.
- **Forensic SNR**: The system monitors the per-satellite Signal-to-Noise Ratio (`snrIdx`). A sudden drop in average SNR while sats remain "in view" provides secondary confirmation. `JAMMER_DETECTION_THRESHOLD_MS` is set to 180s.
- **State Synchronization**: The Jammer violation state is centrally confirmed in the active service using a 180s cumulative threshold (`jumpStateStartTs`).

## 3. Signal Loss & GPS Gap
Determining the difference between a network drop and a GPS fix loss:
- **Signal Loss**: No telemetry received from the peer for >180s (`TRACKER_SIGNAL_LOSS_THRESHOLD_MS`).
- **GPS Gap**: Connection is active, but GPS data is stale for >180s (`GPS_GAP_THRESHOLD_MS`).

## 4. GPS Monotonicity & Continuity (v8.8.35)
- **Merge-on-Stale**: If a packet arrives with stale GPS coordinates but fresh hardware status, the system preserves the status update while holding the Last Known Value (LKV) for location.
- **Clock Regression Guard**: The system strictly rejects GPS updates that move backward in time or arrive out-of-order (`CLOCK_REGRESSION_GATE_MS` 100ms).
- **Data Fidelity**: All telemetry packets and history records include raw **speed** and **bearing**.
- **Monotonic UI Timing**: All UI lockout and overlay thresholds use `elapsedRealtime` to ensure stability across system clock jumps.
- **Forensic Unification**: Legacy `ver` and `vid` tags have been removed to simplify the forensic model while maintaining high-fidelity physics tracking.

## 5. GNSS Detail Tracking
- **Satellite Metadata**: The system broadcasts raw CN0 (SNR) and constellation type for every satellite.
- **Interactive UI**: Tapping the "Sats" card in the HUD allows real-time visualization of signal health.
