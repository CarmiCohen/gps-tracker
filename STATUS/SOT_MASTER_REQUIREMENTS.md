# SOT Master Requirements & Hardening Status (Oct6.13)

## 🏗️ Architectural Master Rules (156 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.121 Tick Preemption (R-ID 289-H)**: Background services MUST implement a tick preemption mechanism to allow safety-critical fast-path triggers (Acoustic/Light) to bypass relaxed intervals and force immediate alarm evaluation. (Oct6.3 - Issue #AUDIT-1006-2).
*   **1.122 Adaptive Telemetry Conflation (R-ID 511)**: Signaling dispatchers MUST implement adaptive conflation for high-frequency location and log bursts to minimize radio usage while preserving forensic sequence integrity via sequence-break flushes. (Oct6.4 - Issue #AUDIT-1006-7).
*   **1.123 Signal Efficiency Auditing (R-ID 511-M)**: The signaling dispatcher MUST maintain atomic metrics for frames received, emitted, and conflated, and EXPOSE these metrics in the System Diagnostics UI to verify radio efficiency gains in field deployments. (Oct6.8 - Issue #AUDIT-1006-8).
*   **1.124 Channel-Based Task Preemption (R-ID 289-M)**: Task preemption for periodic loops MUST use low-overhead synchronization primitives (e.g., Channels) rather than polling to minimize CPU wakeups. (Oct6.7 - Issue #AUDIT-1006-8 / SIMP-1426-5).
*   **1.125 Protocol Delta Encoding (R-ID 511-L)**: Binary signaling protocols SHOULD use delta-encoding for high-resolution coordinate fields (E7) to leverage Protobuf variable-length encoding (zigzag) for typical incremental movements, reducing per-packet radio energy. MUST clear absolute double fields when transmitting deltas to ensure wire-level savings. (Oct6.10 - Issue #AUDIT-1006-9).
*   **1.126 Dispatcher Recovery Lifecycle (R-ID 511-XL)**: Signaling dispatchers MUST implement a reinitialization sequence to recover from terminal states (closed channels) after disconnects. Reconnection logic MUST trigger this recovery to ensure telemetry delivery resumes without requiring a process restart. (Oct6.11 - Issue #AUDIT-1006-10).
*   **1.127 Unified Conflation Management (R-ID 511-XXL)**: Conflation logic for multiple telemetry streams (maps, objects, logs) MUST be consolidated into a single background loop governed by a signal-driven scheduling mechanism. This minimizes coroutine overhead and ensures deterministic timing across diverse data types. (Oct6.12 - SIMP-1426-7).
*   **1.128 Dynamic Conflation Scaling (R-ID 511-P)**: Signaling conflation delays MUST scale dynamically during high-pressure bursts. When telemetry density exceeds a specific threshold (e.g., 5 frames), the dispatcher MUST extend the conflation window to maximize per-packet data density and reduce radio duty cycles. (Oct6.13 - Issue #SIMP-1426-8).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 642**: Dynamic Conflation Pressure Adaptation - Implemented dynamic scaling of conflation delays in `SmartSignalingDispatcher`. The delay extends during high-frequency bursts to maximize radio efficiency while maintaining forensic sequence via sequence-break flushes. (Resolved Oct6.13 - Issue #SIMP-1426-8).
*   **SOT ID 641**: Unified Conflation Loop - Replaced individual conflation jobs with a single signal-driven loop in `SmartSignalingDispatcher`. Reduced coroutine pressure and simplified lifecycle. (Resolved Oct6.12 - SIMP-1426-7).
*   **SOT ID 640**: Dispatcher Lifecycle Recovery - Fixed terminal-state bug where signaling channels remained closed after reconnection. Added reinitialize() to restart processor loop. (Resolved Oct6.11 - Issue #AUDIT-1006-10).
*   **SOT ID 639**: Protocol Delta Refinement - Fixed coordinate reconstruction precision loss and isolated signaling delta state from persistence to prevent reference corruption. (Resolved Oct6.10 - Issue #AUDIT-1006-9).
*   **SOT ID 638**: Reactive Signaling Metrics - Replaced polling with StateFlow observation for dispatcher telemetry to reduce binder traffic and improve UI reactivity. (Resolved Oct6.9 - SIMP-1426-6).
*   **SOT ID 637**: Signal Metrics UI - Exposed real-time conflation efficiency and radio emission counts in DiagnosticsScreen for field audit. (Resolved Oct6.8).

---

## 🏁 Verification Chapters
*   **Chapter 31.259 (Dynamic Conflation Audit)**: PASSED - Verified that conflation delays correctly scale during high-frequency bursts. Confirmed that sequence-break flushes (e.g., different log messages) reset the pressure counters and dispatch immediately. (Oct6.13)
*   **Chapter 31.258 (Conflation Consolidation Audit)**: PASSED - Verified that a single `conflationJob` correctly handles location maps, location objects, and logs. Confirmed that conflation delays are applied accurately and metrics reflect proper aggregation. (Oct6.12)
*   **Chapter 31.257 (Signaling Recovery Audit)**: PASSED - Verified that after calling disconnect() and then connect(), the dispatcher correctly recreates channels and resumes processing commands. No dropped frames observed post-reconnection. (Oct6.11)
*   **Chapter 31.256 (Protocol Precision Audit)**: PASSED - Verified coordinate reconstruction using floating-point math. Confirmed 7-decimal place fidelity after E7 delta expansion. (Oct6.10)
*   **Chapter 31.255 (Protocol Efficiency Audit)**: PASSED - Verified coordinate delta-encoding. Consecutive binary frames show significant size reduction for small movements. Absolute double fields are correctly omitted (set to 0.0) during delta frames. (Oct6.10)
*   **Chapter 31.254 (Field Signaling Audit)**: PASSED - Verified that signaling metrics are rendered in DiagnosticsScreen. Savings percentage correctly calculates based on received/conflated frames. (Oct6.8)
