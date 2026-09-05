# Resolution Archive (Sep.05.28)

## 🟢 Resolved Issues (Sep.05.28)
*   **Issue #914 RESOLVED: GNSS Detail Sampling**. Implemented A15-aware sampling for the `activeGnssDetail` flow in `MainViewModel`. This optimization reduces UI overhead during satellite monitoring by throttling update frequencies to 3000ms on budget devices, ensuring interface responsiveness under high system load (R-ID 267).

## 🟢 Resolved Issues (Sep.05.27)
*   **Issue #918 RESOLVED: VWR Badge Consistency & Verification**. Standardized all peer activity tracking and staleness evaluation to strictly use monotonic `elapsedRealtime()`. Remediated a clock-source mismatch where wall-clock timestamps were compared against monotonic gates, ensuring the 35s RED transition is robust and accurate across all HUD indicators (R-ID 257).

## 🟢 Resolved Issues (Sep.05.26)
*   **Issue #912 RESOLVED: WebSocket Fallback**. Re-enabled XHR polling fallback by allowing transport negotiation (`polling` to `websocket`) in `CommunicationManager`. This ensures connectivity on restricted networks that block direct WebSocket upgrades, while maintaining the 60s timeout for instance spin-up to accommodate Render free tier latency (R-ID 251).

## 🟢 Resolved Issues (Sep.05.25)
*   **Issue #266 RESOLVED: Mali Driver Mitigation**. Implemented automated detection of Mali driver instability in `IntegrityMonitor` (correlated via CPU load and I/O latency on budget hardware). Propagated state through the HUD aggregator and implemented reactive UI-throttling in `SharedUiComponents` (suppressing high-frequency animations) to prevent process-level ANRs on budget devices (R-ID 266).
*   **Issue #910 RESOLVED: Hydration Watchdog**. Implemented a 15s hydration watchdog (`WATCH_DOG_UI_GRACE_MS`) in `MainViewModel`. The system now monitors startup hydration levels and forces the UI initialization state if a hang is detected at Level 2, ensuring a recovery path from black-screen locks on budget hardware (R-ID 261).

## 🟢 Resolved Issues (Sep.05.24)
*   **Issue #916 RESOLVED: GNSS Revival Lifecycle Transparency & Battery Audit**. Finalized the GNSS recovery event chain in `HardwareProvider.kt` and instrumented `IntegrityMonitor.kt` to log battery current (mA) snapshots during revival pulses (Sep.05.23).
*   **Issue #918 RESOLVED: Clock Source Consistency**. Standardized all staleness gates to strictly use `SystemClock.elapsedRealtime()` to ensure HUD accuracy across system clock jumps (Sep.05.23).
*   **Issue #918 RESOLVED: VWR Badge Persistence Leak**. Restricted activity indicators to high-assurance telemetry packets (Location/Health) to prevent persistence leaks (Sep.05.23).
*   **Issue #917 RESOLVED: Exact Actual Colors (HUD LED Synchronization)**. Standardized all HUD LEDs to monitor actual service pulses with a synchronized 35s staleness gate (Sep.05.23).
*   **Issue #915 RESOLVED: Mapnik Tile Latency (Samsung A15)**. Expanded disk cache to 600MB and throttled concurrency for budget hardware (Sep.05.23).
*   **Issue #910 RESOLVED: Forensic Stall Simulation (Service Termination Race)**. Hardened navigation guards to prevent Landing retreats during hydration (Sep.05.23).
