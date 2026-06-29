# GPS Revival & Advanced Monitoring Mechanisms (v8.9.54)

This document explains the specialized monitoring logic used to detect "silent" failures like GPS stalling, signal jamming, and the mechanisms used to "revive" data flow.

## 1. GPS Stalling Detection & Escalated Revival
**Problem**: Sometimes the GPS hardware remains "active" but stops providing new coordinates, or provides old "stale" data repeatedly.

**Mechanism**:
- **Stall Watchdog**: The `LocationProcessor` tracks the raw hardware timestamp. If the coordinates are identical across updates while vibration sensors indicate movement, it flags a stall.
- **UI Threshold (15s)**: The UI uses `GPS_UI_FAIL_THRESHOLD_MS` (15 seconds) as a strict "Position Health" gate. This threshold is relaxed from the 10s mandate to accommodate network jitter (Issue #428).
- **Instant Recovery (R923)**: Freshness logic utilizes the maximum of the GPS timestamp and the telemetry arrival timestamp (`telemetryTs`) to prevent dashboard "gray-out" during link restoration.
- **Escalated Revival (Issue #341)**: Sustained stalls beyond `GPS_STALL_THRESHOLD_MS` (60s) trigger a hardware-level refresh cycle:
    - **Retry Loop**: The system attempts to revive the GPS hardware every 120 seconds (`GPS_REVIVAL_RETRY_INTERVAL_MS`).
    - **Max Attempts**: After 3 failed attempts (`MAX_REVIVAL_ATTEMPTS`), the system escalates to a **CRITICAL forensic alert**.
    - **Viewer Notification**: A `CRITICAL: GPS_HARDWARE_LOCK` log is emitted to signal that manual intervention or physical relocation is required.
    - **Dual-Metric Anchor (Issue #325)**: These critical revival logs are automatically anchored with `lat`/`lng`, `accuracy`, and authoritative `maxAccuracy`.

## 2. Jammer Detection & Hardening (v8.9.54)
**Problem**: Sophisticated jammers might block GPS signals while leaving data connections active.

**Mechanism**:
- **Cross-Validation**: The system monitors the relay connection independently of the GNSS fix.
- **Suspicion Trigger**: If the device is connected to the relay but hasn't received a fresh GPS update while high vibration indicates movement, it triggers `isJammerSuspicion`.
- **Forensic SNR (Issue #332)**: The system monitors the per-satellite Signal-to-Noise Ratio. Sudden SNR drops while satellites remain "in view" provide secondary confirmation.
- **SNR Latch (Issue #452)**: Signal reflections with high SNR (≥35) trigger a **6-minute (360s)** security hold to prevent siren flickering.
- **State Synchronization (Issue #315)**: Jammer violation state is centrally confirmed using `JAMMER_DETECTION_THRESHOLD_MS` (180s).

## 3. Signal Loss & GPS Gap
Determining the difference between a network drop and a GPS fix loss:
- **Signal Loss**: No telemetry received from the peer for >180s (`TRACKER_SIGNAL_LOSS_THRESHOLD_MS`).
- **GPS Gap**: Connection is active, but GPS data is stale for >60s (`GPS_GAP_THRESHOLD_MS`).

## 4. GPS Monotonicity & Continuity (v8.9.54)
- **Merge-on-Stale**: Preserves hardware status while holding the Last Known Value (LKV) for location.
- **Clock Regression Guard**: Rejects updates that move backward in time (`CLOCK_REGRESSION_GATE_MS` 100ms).
- **Data Fidelity**: All telemetry packets and history records include raw **speed** and **bearing**.
- **Log Spatial Anchor (Issue #325)**: All system events and continuity gaps are now geographically anchored with Dual-Metric accuracy for map reconstruction.
- **Ghost Mode UX (Issue #338)**: Visual staleness indicators applied when telemetry > 15s old.
