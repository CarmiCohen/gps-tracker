# SOT Master Requirements & Hardening Status (Oct.5.5)

## 🏗️ Architectural Master Rules (132 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.107 Forensic Diagnostic Expansion (R1344)**: System diagnostic snapshots (`thermalSnapshot`, `heapSnapshot`) MUST be captured and propagated through all telemetry and logging channels. Diagnostic data MUST be merged during history ribbon aggregation to preserve peak stress metrics. Alarm logs MUST carry these snapshots to enable remote correlation of hardware load with trajectory anomalies. (Oct.5.2 - Issue #1344).
*   **1.108 Native FastPath Convergence (R1510-2)**: ALL high-frequency (100Hz+) sensor math primitives, including vector magnitude, high-pass filtering, and energy EMA, MUST be offloaded to the native C++ layer (`jdHardware`). Violation gates for vibration and shock MUST be evaluated natively with CPU-load awareness to eliminate JVM floating-point overhead and battery drain during stationary monitoring. (Oct.5.5 - Issue #SIMP-1510-1).
*   **1.109 Prioritized Event Bus (R1328)**: The `DomainEventBus` MUST maintain a minimum buffer capacity of 512 items to handle 100Hz forensic bursts. ALL `DomainEvent` descendants MUST carry an `EventPriority` metadata field (CRITICAL, HIGH, NORMAL, LOW) to support deterministic backpressure handling and ensure critical alarms are never dropped during buffer saturation. (Oct.5.5 - Issue #1328).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 619**: Prioritized Event Bus - Increased DomainEventBus capacity to 512 and implemented EventPriority metadata across the hierarchy. (Resolved Oct.5.5).
*   **SOT ID 618**: Native FastPath Convergence (Phase 2) - Fully migrated the 100Hz vibration pipeline to JNI, including magnitude, HPF, Energy, and Violation Gates. (Resolved Oct.5.5).
*   **SOT ID 617**: Forensic Diagnostic Expansion - Integrated thermal and memory snapshots across all telemetry aggregation and logging paths. (Resolved Oct.5.2).

---

## 🏁 Verification Chapters
*   **Chapter 31.238 (Event Bus Backpressure Audit)**: PASSED - Verified buffer expansion to 512 items in `DomainEventBus`. Confirmed `EventPriority` implementation in `EngineModels.kt` for all event types. Validated non-blocking emission during simulated 100Hz bursts. (Oct.5.5)
*   **Chapter 31.237 (JNI Vibration Audit)**: PASSED - Verified native offloading of magnitude, HPF, and energy math at 100Hz in `HardwareSuite`. Confirmed violation gate parity between JVM and C++ in `SentinelValidator`. (Oct.5.5)
*   **Chapter 31.236 (Forensic Expansion Audit)**: PASSED - Verified snapshot capture in `IntegrityMonitor`. (Oct.5.2)
