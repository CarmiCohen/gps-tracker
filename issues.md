# Project Issues & Hardening Tracking (Sep.05.25)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.05.25)
*   **Issue #266 RESOLVED: Mali Driver Mitigation**. Implemented automated detection of Mali driver instability in `IntegrityMonitor` (correlated via CPU load and I/O latency on budget hardware). Propagated state through the HUD aggregator and implemented reactive UI-throttling in `SharedUiComponents` (suppressing high-frequency animations) to prevent process-level ANRs on budget devices (R-ID 266).
*   **Issue #910 RESOLVED: Hydration Watchdog**. Implemented a 15s hydration watchdog (`WATCH_DOG_UI_GRACE_MS`) in `MainViewModel`. The system now monitors startup hydration levels and forces the UI initialization state if a hang is detected at Level 2, ensuring a recovery path from black-screen locks on budget hardware (R-ID 261).

## 🟢 Recently Resolved Issues (Sep.05.24)
*   **Issue #916 RESOLVED: GNSS Revival Lifecycle Transparency & Battery Audit**. Finalized the GNSS recovery event chain in `HardwareProvider.kt` and instrumented `IntegrityMonitor.kt` to log battery current (mA) snapshots during revival pulses (Sep.05.23).
*   **Issue #918 RESOLVED: Clock Source Consistency**. Standardized all staleness gates to strictly use `SystemClock.elapsedRealtime()` to ensure HUD accuracy across system clock jumps (Sep.05.23).
*   **Issue #918 RESOLVED: VWR Badge Persistence Leak**. Restricted activity indicators to high-assurance telemetry packets (Location/Health) to prevent persistence leaks (Sep.05.23).
*   **Issue #917 RESOLVED: Exact Actual Colors (HUD LED Synchronization)**. Standardized all HUD LEDs to monitor actual service pulses with a synchronized 35s staleness gate (Sep.05.23).
*   **Issue #915 RESOLVED: Mapnik Tile Latency (Samsung A15)**. Expanded disk cache to 600MB and throttled concurrency for budget hardware (Sep.05.23).
*   **Issue #910 RESOLVED: Forensic Stall Simulation (Service Termination Race)**. Hardened navigation guards to prevent Landing retreats during hydration (Sep.05.23).

## 🟡 Open Issues & Hardening Tasks (vSep.05.25)
*   **Issue #912: WebSocket Fallback**. [EDGE CASE] [MEDIUM] Strict `websocket` transport bypasses polling but fails on restricted networks. Intelligent XHR fallback is required (R-ID 251).
*   **Issue #918 Verification**: [PENDING] [MEDIUM] Physical confirmation of the 35s transition to RED for the VWR badge under signal stress.
*   **Issue #914: GNSS Detail Sampling**. [INCONSISTENCY] [LOW] The `gnssDetail` flow is not sampled. High-frequency updates may cause UI overhead on A15 devices.
*   **Issue #916: Energy Footprint Verdict**. [MISSING] [LOW] Implement automated mA delta and temperature rise calculation to quantify revival power cost (R-ID 259).
*   **R-ID 256: Sensor Rate Verification**. [PENDING] [LOW] Add a runtime check to verify `HIGH_SAMPLING_RATE_SENSORS` efficacy on Target SDK 35.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 273 (Rules: 46, IDs: 227), Resolved: 901, Open: 5, Testing: 92% (Chapters), Ideas: 214, QA: 242]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.05.25)*
