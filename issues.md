# Project Issues & Hardening Tracking (Sep.05.29)

## 🎯 Current Resumption Focus: Physical Verification & Forensic Closing
Finalizing the high-assurance baseline for Samsung A15 hardware and signaling transport.

## 🟢 Recently Resolved Issues (Sep.05.29)
*   **Issue #921 RESOLVED: Sensor Rate Verification**. Implemented a runtime efficacy audit in `HardwareProvider` to verify `HIGH_SAMPLING_RATE_SENSORS` performance on Target SDK 35. The system now measures actual Hz after the stabilization window and logs the efficacy status (True if >200Hz) to forensic logs, ensuring high-fidelity IMU data collection is not throttled by the OS (R-ID 256).

## 🟢 Recently Resolved Issues (Sep.05.28)
*   **Issue #914 RESOLVED: GNSS Detail Sampling**. Implemented A15-aware sampling for the `activeGnssDetail` flow in `MainViewModel`. This optimization reduces UI overhead during satellite monitoring by throttling update frequencies to 3000ms on budget devices, ensuring interface responsiveness under high system load (R-ID 267).

## 🟢 Recently Resolved Issues (Sep.05.27)
*   **Issue #918 RESOLVED: VWR Badge Consistency & Verification**. Standardized all peer activity tracking and staleness evaluation to strictly use monotonic `elapsedRealtime()`. Remediated a clock-source mismatch where wall-clock timestamps were compared against monotonic gates, ensuring the 35s RED transition is robust and accurate across all HUD indicators (R-ID 257).

## 🟢 Recently Resolved Issues (Sep.05.26)
*   **Issue #912 RESOLVED: WebSocket Fallback**. Re-enabled XHR polling fallback by allowing transport negotiation (`polling` to `websocket`) in `CommunicationManager`. This ensures connectivity on restricted networks that block direct WebSocket upgrades, while maintaining the 60s timeout for instance spin-up (R-ID 251).

## 🟢 Recently Resolved Issues (Sep.05.25)
*   **Issue #266 RESOLVED: Mali Driver Mitigation**. Implemented automated detection of Mali driver instability in `IntegrityMonitor`. Propagated state through the HUD aggregator and implemented reactive UI-throttling in `SharedUiComponents` to prevent process-level ANRs on budget devices (R-ID 266).

## 🟡 Open Issues & Hardening Tasks (vSep.05.29)
*   **Issue #916: Energy Footprint Verdict**. [MISSING] [LOW] Implement automated mA delta and temperature rise calculation to quantify revival power cost (R-ID 259).

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 274 (Rules: 46, IDs: 228), Resolved: 905, Open: 1, Testing: 94% (Chapters), Ideas: 216, QA: 243]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.05.29)*
