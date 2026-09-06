# Resolution Archive (Sep.05.30)

## 🟢 Resolved Issues (Sep.05.30)
*   **Issue #916 RESOLVED: Energy Footprint Verdict**. Implemented automated mA delta and temperature rise calculation in `HardwareProvider` to quantify the power cost of GNSS revival cycles. The system now captures instantaneous battery snapshots via `SystemStatusProvider` at the start and end of recovery bursts, logging a definitive energy footprint to forensic logs (R-ID 259).

## 🟢 Resolved Issues (Sep.05.29)
*   **Issue #921 RESOLVED: Sensor Rate Verification**. Implemented a runtime efficacy audit in `HardwareProvider` to verify `HIGH_SAMPLING_RATE_SENSORS` performance on Target SDK 35. The system now measures actual Hz after the stabilization window and logs the efficacy status (True if >200Hz) to forensic logs, ensuring high-fidelity IMU data collection is not throttled by the OS (R-ID 256).

## 🟢 Resolved Issues (Sep.05.28)
*   **Issue #914 RESOLVED: GNSS Detail Sampling**. Implemented A15-aware sampling for the `activeGnssDetail` flow in `MainViewModel`. This optimization reduces UI overhead during satellite monitoring by throttling update frequencies to 3000ms on budget devices, ensuring interface responsiveness under high system load (R-ID 267).

## 🟢 Resolved Issues (Sep.05.27)
*   **Issue #918 RESOLVED: VWR Badge Consistency & Verification**. Standardized all peer activity tracking and staleness evaluation to strictly use monotonic `elapsedRealtime()`. Remediated a clock-source mismatch where wall-clock timestamps were compared against monotonic gates, ensuring the 35s RED transition is robust and accurate across all HUD indicators (R-ID 257).

## 🟢 Resolved Issues (Sep.05.26)
*   **Issue #912 RESOLVED: WebSocket Fallback**. Re-enabled XHR polling fallback by allowing transport negotiation (`polling` to `websocket`) in `CommunicationManager`. This ensures connectivity on restricted networks that block direct WebSocket upgrades, while maintaining the 60s timeout for instance spin-up to accommodate Render free tier latency (R-ID 251).

## 🟢 Resolved Issues (Sep.05.25)
*   **Issue #266 RESOLVED: Mali Driver Mitigation**. Implemented automated detection of Mali driver instability in `IntegrityMonitor` (correlated via CPU load and I/O latency on budget hardware). Propagated state through the HUD aggregator and implemented reactive UI-throttling in `SharedUiComponents` (suppressing high-frequency animations) to prevent process-level ANRs on budget devices (R-ID 266).
*   **Issue #910 RESOLVED: Hydration Watchdog**. Implemented a 15s hydration watchdog (`WATCH_DOG_UI_GRACE_MS`) in `MainViewModel`. The system now monitors startup hydration levels and forces the UI initialization state if a hang is detected at Level 2, ensuring a recovery path from black-screen locks on budget hardware (R-ID 261).

*(For older resolutions, see history logs.)*
