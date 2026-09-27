# 🏛️ Resolution Archive - Sep.27.6

## 🏁 Issue #1161: Unified Trajectory & Buffer Management
*   **Resolved**: Sep.27.6
*   **Root Cause**: Trajectory data was scattered across redundant primitive arrays in `GtoBufferState` and conceptually overlapping "hindsight" buffers in `LocationSentinel`, leading to dual terminology and suboptimal memory access patterns.
*   **Remediation**:
    *   **EngineModels.kt**: Introduced `TrajectoryBuffer` and `TrajectoryNode` to replace `GtoBufferState` and `GtoNode`. Unified all trajectory-related telemetry (lat, lng, alt, accuracy, maxAccuracy, bearing, speed, ts, rt, vibe) into a single optimized ring buffer.
    *   **EngineConstants.kt**: Unified capacity and age constants under `TRAJECTORY_BUFFER_MAX_SIZE` and `TRAJECTORY_HINDSIGHT_MAX_AGE_MS`.
    *   **GtoEngine.kt**: Refactored `evaluateTrajectory` and `addPoint` to operate on the unified `TrajectoryBuffer`.
    *   **LocationSentinel.kt**: Updated `getHindsightBuffer` to utilize the unified mapping from `TrajectoryNode` to `RejectedPoint`.
    *   **Validation**: Successfully executed `:core:engine:test` ensuring zero regressions in trajectory promotion logic.
*   **SOT ID**: 506

# 🏛️ Resolution Archive - Sep.27.5

## 🏁 Issue #1349: LocationProcessingState Mutability Reduction
*   **Resolved**: Sep.27.5
*   **Root Cause**: `LocationProcessingState` was a monolithic data class containing mixed concerns (accuracy tracking, forensic telemetry, trajectory buffers, and anchor logic), leading to high mutability and poor domain isolation.
*   **Remediation**:
    *   **EngineModels.kt**: Partitioned `LocationProcessingState` into four specialized sub-states: `AccuracyState`, `SentinelForensicState`, `GtoBufferState` (now `TrajectoryBuffer`), and `AnchorState`.
    *   **Logic Alignment**: Refactored `LocationProcessor`, `LocationSentinel`, `AnchorEvaluator`, and `GtoEngine` to utilize these sub-states, improving field-level access decoupling and reducing the monolithic footprint.
    *   **Cleanup**: Removed redundant naming prefixes (e.g., `sentinelLastValidLat` converted to `forensic.lastValidLat`) to simplify the internal processing API.
    *   **App Integration**: Updated `MonitorService` and tests to ensure full parity with the partitioned hierarchy.
*   **SOT ID**: 505

# 🏛️ Resolution Archive - Sep.27.4

## 🏁 Issue #1348: DomainEvent Hierarchy Simplification
*   **Resolved**: Sep.27.4
*   **Root Cause**: The `DomainEvent` hierarchy used nested wrapper classes (e.g., `DomainEvent.Alarm(AlarmEvent)`) which introduced unnecessary object allocations and required two-step pattern matching in the `AppEventCoordinator`, increasing dispatch latency in high-frequency tracking scenarios.
*   **Remediation**:
    *   **EngineModels.kt**: Refactored the sealed hierarchy. Component-level event classes now inherit directly from `DomainEvent`.
    *   **LocationProcessor.kt**: Added `isPrimary` flag to constructor to support direct emission of contextualized events.
    *   **AppEventCoordinator.kt**: Simplified `observeDomainEvents` to pattern match on flattened events.
    *   **Call Sites**: Updated `MonitorService`, `ConnectivitySuite`, `IntegrityMonitor`, `CommandRouter`, and `AppAlarmManager` to emit flattened events.
*   **SOT ID**: N/A (Architectural Simplification)

# 🏛️ Resolution Archive - Sep.27.3

## 🏁 Issue #1347: Audit and Itemize Strategic Simplification Candidates
*   **Resolved**: Sep.27.3
*   **Root Cause**: Backlog drift and legacy complexity markers (e.g., decommissioned services and redundant mappers) created overhead and architectural noise, requiring a formal audit to align with the "Ideas: 15" hardening goal.
*   **Remediation**:
    *   **issues.md**: Audited and pruned the "Strategic Simplification Ideas" list to 15 active candidates. Added high-priority candidates for Trajectory/Buffer unification and Smart Dispatcher consolidation.
    *   **Audit**: Verified that `TrackerService` and `ViewerService` are fully decommissioned stubs and removed their active references from manifest-level logic in previous turns.
    *   **Architecture**: Cataloged new simplification opportunities including `DomainEvent` hierarchy flattening and `LocationProcessingState` mutability reduction.
*   **SOT ID**: N/A (Process Audit)

# 🏛️ Resolution Archive - Sep.27.2

## 🏁 Issue #1345: Expand Automated Network Stress Tests
*   **Resolved**: Sep.27.2
*   **Root Cause**: Lack of high-frequency signaling stress testing to verify resource stability during extreme network instability (e.g., rapid toggling between WiFi and Cellular).
*   **Remediation**:
    *   **ConnectivitySuite.kt**: Implemented `executeFlappingStressTest` to simulate a 10s signaling burst at 5Hz.
    *   **MonitorService.kt**: Integrated `ExecuteNetworkStressTest` command handling to trigger the flapping simulation.
    *   **EngineModels.kt**: Added `ExecuteNetworkStressTest` to the `CommandEvent` hierarchy.
*   **SOT ID**: 503

# 🏛️ Resolution Archive - Sep.26.12

## 🏁 Issue #1344: Implement Thermal & Memory Forensic Probes
*   **Resolved**: Sep.26.12
*   **Remediation**: Integrated OS-level thermal headroom and heap utilization monitoring into the periodic integrity heartbeat loop. Expanded binary forensic trace schema to version 4 to support high-assurance hardware stability auditing during long-duration soak tests. (SOT ID 502).
