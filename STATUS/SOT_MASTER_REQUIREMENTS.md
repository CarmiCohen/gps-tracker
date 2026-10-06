# SOT Master Requirements & Hardening Status (Oct6.15)

## 🏗️ Architectural Master Rules (158 Rules)

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
*   **1.129 Conflation State Consolidation (R-ID 511-Q)**: Conflation state (pending payloads, scheduled timestamps, burst counts) MUST be encapsulated in a unified container (e.g., ConflationBucket) to ensure atomic state transitions, simplify lifecycle reinitialization, and reduce boilerplate in scheduling logic. (Oct6.14 - Issue #SIMP-1426-9).
*   **1.130 Protobuf Stream Compression (R-ID 511-R)**: Binary signaling payloads exceeding 512 bytes SHOULD be compressed (e.g., using Gzip) before transmission. The relay protocol MUST include a compression flag in the header to allow transparent decompression at the viewer or server. (Oct6.15 - Issue #AUDIT-1006-11).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 644**: Protobuf Stream Compression - Initiated implementation of wire-level compression for large binary payloads. (Oct6.15 - Issue #AUDIT-1006-11).
*   **SOT ID 643**: Conflation State Consolidation - Refactored `SmartSignalingDispatcher` to use a unified `ConflationBucket` structure. Hardened lifecycle by ensuring signal channels are recreated during reinitialization. (Resolved Oct6.14 - Issue #SIMP-1426-9).
*   **SOT ID 642**: Dynamic Conflation Pressure Adaptation - Implemented dynamic scaling of conflation delays in `SmartSignalingDispatcher`. (Resolved Oct6.13 - Issue #SIMP-1426-8).
*   **SOT ID 641**: Unified Conflation Loop - Replaced individual conflation jobs with a single signal-driven loop in `SmartSignalingDispatcher`. (Resolved Oct6.12 - SIMP-1426-7).

---

## 🏁 Verification Chapters
*   **Chapter 31.261 (Signaling Stress Audit)**: PASSED - Verified that `SmartSignalingDispatcher` correctly handles interleaved bursts of all telemetry types without state collisions or message loss. Confirmed memory safety of `ConflationBucket` reset logic. (Oct6.15 - Issue #TEST-1006-1).
*   **Chapter 31.260 (Conflation Architecture Audit)**: PASSED - Verified that `ConflationBucket` correctly encapsulates state and that `reinitialize()` successfully clears and restarts the signaling pipeline. Confirmed that pressure adaptation remains active and correct. (Oct6.14)
*   **Chapter 31.259 (Dynamic Conflation Audit)**: PASSED - Verified that conflation delays correctly scale during high-frequency bursts. (Oct6.13)
