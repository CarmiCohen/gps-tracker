# 📜 Resolution Archive

## 🟢 Resolved in Oct.5.7
*   **Issue #1450: JNI Math Batching.** Consolidated granular vibration math calls (magnitude, HPF, energy, floor update, stationary gate) into a single 256-byte `DirectByteBuffer` transaction (`n19`). This minimizes JNI bridge transitions from 5 calls per tick to 1, significantly hardening the 100Hz hot-path and reducing CPU context switching overhead (R1450).

## 🟢 Resolved in Oct.5.6
*   **Issue #1328: Event Bus Backpressure & UI Performance Hardening.** Implemented a multi-tier backpressure mitigation strategy for high-frequency forensic sampling (100Hz). 
    *   **Prioritized Drop Strategy**: Modified `DomainEventBus.emit()` to drop `LOW` priority events (e.g., non-critical logging) when the reactive bus detects high subscription pressure (threshold: 5), protecting the tracking engine's hot-path from UI-induced stalls.
    *   **UI Recomposition Hardening**: Refactored `MainAppContent`, `TrackerScreen`, and `ViewerScreen` to use granular state slicing (R1422). Moved collection of high-frequency transient states (`KinematicState`, `DiagnosticState`) from the root UI to specialized leaf screens. This isolates volatile triggers (like battery temp or RTT updates) and prevents redundant recompositions of the entire UI tree during 100Hz vibration bursts. (Oct.5.6 - Issue #1328).

## 🟢 Resolved in Oct.5.5
*   **Issue #SIMP-1510-1: Native FastPath Convergence (Phase 2).** Fully migrated the high-frequency vibration pipeline (100Hz) to JNI. Offloaded vector magnitude, HPF, Kinetic Energy, and violation gates to `jdHardware` C++ layer. Hardened coefficients to match `EngineConstants.kt` and eliminated JVM floating-point math from hot paths to reduce CPU churn and battery consumption (R1510-2).

## 🟢 Resolved in Oct.5.2
*   **Issue #1344: Forensic Diagnostic Expansion.** Integrated `thermalSnapshot` and `heapSnapshot` into `EngineConnectionPoint`, `RealtimeStatus`, and `TrackerStatusProto`. Updated `IntegrityMonitor` to capture high-fidelity snapshots of thermal headroom and heap allocation during heartbeats. Synchronized `TelemetryMapper` and `ForensicSpillBuffer` to preserve these snapshots in binary history and offline buffers, enabling remote correlation of system stress with trajectory behavior (R1344).

## 🟢 Resolved in Oct.5.1
*   **Issue #SIMP-1201-1: Logic State Serialization Expansion.** Refactored `AlarmEvaluationState` persistence to use a consolidated Protobuf map (`role_logic_states`) in DataStore. Eliminated parameter bloat in `saveLogicState` and implemented binary-safe recovery for monotonic timestamps and siren lockout states, ensuring forensic continuity across service restarts (R1201).
*   **Issue #1173: Protobuf-First Persistence (Phase 2).** Completed the transition to binary persistence for connection history and pending updates. Implemented high-performance binary restoration fallbacks in `TelemetryMapper` and on-the-fly migration for legacy SQLite columns in `OfflineRepository`. Fully decoupled storage from Room schema complexity (R1173).

## 🟢 Resolved in Oct.4.6
*   **Issue #1160: Flyweight & Pooling Expansion.** Implemented `RingBufferPool` and centralized `EnginePools` for `LocationUpdate`, `ProcessedLocation`, `SystemHealthState`, and `TrajectoryNode`. Refactored high-frequency paths in `MonitorService` and `LocationProcessor` to use pooled acquisition, eliminating GC churn during long-duration alerts and 100Hz sampling (R1160).
