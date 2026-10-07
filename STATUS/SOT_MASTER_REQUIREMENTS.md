# SOT Master Requirements & Hardening Status (Oct7.5)

## 🏗️ Architectural Master Rules (163 Rules)

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
*   **1.131 SignalingPipeline Abstraction (R-ID 511-S)**: Wire-level optimizations (Protobuf serialization, delta-encoding state, compression) MUST be encapsulated in a dedicated SignalingPipeline abstraction. Transport providers (e.g., CommunicationManager) MUST delegate transmission to the pipeline to ensure separation of concerns and instance-bound state management. (Oct6.20 - Issue #SIGN-1006-12).
*   **1.132 Conflation Strategy Consolidation (R-ID 511-T)**: Field-level conflation strategies for various data types (Maps, LocationUpdates, Logs) MUST be encapsulated within the `SignalingPipeline` implementation to minimize cross-module coupling and centralize protocol optimization logic. (Oct6.23 - Issue #SIGN-1006-13).
*   **1.133 Telemetry Pruning & Serialization Scoping (R-ID 511-U)**: Data models shared between engine evaluation and signaling MUST use serialization-level scoping (e.g., `@Transient`) to exclude internal evaluation scratchpad fields from wire payloads. This ensures minimal radio overhead for JSON-based metadata channels while maintaining a unified domain monolith. (Oct7.1 - Issue #SIMP-1006-14).
*   **1.134 Conflation Starvation Protection (R-ID 511-V)**: Signaling dispatchers MUST implement a starvation cap for dynamic conflation windows. The transmission deadline MUST be calculated relative to the arrival of the FIRST message in a burst to ensure a deterministic maximum latency (e.g., 2000ms) regardless of subsequent burst density. (Oct7.3 - Issue #QA-1007-1).
*   **1.135 Unified Diagnostic Snapshots (R-ID 651)**: Diagnostic sensor probes (SNR, Vibration, Thermal, Heap) MUST be grouped into a unified immutable-friendly container (e.g., `ForensicSnapshot`) across all domain and UI models. This ensures atomic updates, simplifies state duplication (`duplicate()`), and reduces delegation boilerplate in the telemetry monolith. (Oct7.5 - Issue #SIMP-1007-15).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 651**: Unified Snapshot Container - Migrated all engine and app-level diagnostic probes into a grouped `ForensicSnapshot` container for architectural parity. (Oct7.5 - Issue #SIMP-1007-15).
*   **SOT ID 650**: Forensic Telemetry Expansion - Promoted internal engine flags (muzzled, siren, hardware health, environmental lockouts) to Protobuf for remote diagnostics. (Oct7.3 - Issue #QA-1007-1).
*   **SOT ID 649**: Conflation Starvation Protection - Implemented first-entry relative deadlines in `SmartSignalingDispatcher`. (Oct7.3 - Issue #QA-1007-1).
*   **SOT ID 648**: Diagnostic UI Hardening - Corrected label mapping for Exact Alarms and verified signaling efficiency metrics. (Oct7.2 - Issue #QA-1006-12).
*   **SOT ID 647**: Telemetry Pruning - Marked internal engine evaluation fields as transient to optimize JSON wire payloads. (Oct7.1 - Issue #SIMP-1006-14).

---

## 🏁 Verification Chapters
*   **Chapter 31.267 (Forensic Container Parity Audit)**: PASSED - Verified that `EngineConnectionPoint`, `ConnectionPoint`, and `LogEntry` all delegate correctly to the unified `ForensicSnapshot`. Verified that `duplicate()` and `reset()` logic preserves snapshot integrity. (Oct7.5 - Issue #SIMP-1007-15).
*   **Chapter 31.266 (Forensic Fidelity Audit)**: PASSED - Verified that internal engine flags (muzzled, siren, snapshots) are preserved during pipeline emission and correctly mapped to Protobuf. (Oct7.3 - Issue #QA-1007-1).
*   **Chapter 31.265 (Diagnostic Label Audit)**: PASSED - Verified that the "Exact Alarm" item in the Diagnostics UI correctly reports the system permission state. (Oct7.2 - Issue #QA-1006-12).
*   **Chapter 31.264 (Wire Payload Optimization Audit)**: PASSED - Verified that `nowRt`, `nowTs`, and evaluation scratchpad fields are excluded from JSON serialization in `LocationUpdate`. Payload size reduced by ~15% for metadata updates. (Oct7.1 - Issue #SIMP-1006-14).
