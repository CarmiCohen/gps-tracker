# 📜 Resolution Archive

## 🟢 Resolved in Oct6.4
*   **Issue #AUDIT-1006-7: Telemetry Conflation Audit.** Enhanced `SignalingMessageConflator` with deep-merge logic to prevent telemetry fidelity loss (e.g., preserving battery/thermal snapshots when new location updates arrive). Integrated log burst conflation in `SmartSignalingDispatcher` to merge identical normal-priority log entries, significantly reducing radio chatter while maintaining forensic sequence integrity via sequence-break flushes. (Oct6.4 - Rule 1.122 / R-ID 511).

## 🟢 Resolved in Oct6.3
*   **Issue #AUDIT-1006-2: Background Service Transition Latency.** Implemented tick preemption in `MonitorService` and `TickOrchestrator`. Acoustic and light sensor spikes now force an immediate engine tick, bypassing the 5s/15s memory-throttled relaxation intervals. This ensures zero-latency alarm detection and instant `AlarmOverlayService` activation even in low-memory states. (Oct6.3 - Rule 1.121).

## 🟢 Resolved in Oct6.2
*   **Issue #AUDIT-1006-5: Forensic Log Pressure Hardening.** Refactored `LogRepository.addLog` to prevent dropping critical safety alerts (`isImportant = true`) when the `logBuffer` is full. Implemented a non-blocking `trySend` for standard logs and an async `send` fallback for critical alerts. (Oct6.2 - Rule 1.119).
*   **Issue #AUDIT-1006-6: Memory Pressure Throttling.** Enhanced `MonitorService.getRequiredTickInterval()` to respect `MemoryPressureLevel`. Background loops now throttle to 15s during `CRITICAL` pressure and 5s during `HIGH` pressure to prevent background OOM. (Oct6.2 - Rule 1.120).
*   **Architecture & Simulation Hooks.** Added `ExecuteLogPressureTest` and `SetMemoryPressureSimulation` to `UiEvent` and `UiCommand`. Updated `DiagnosticsScreen` with validation hooks for forensic auditing of OOM-prevention and log integrity logic. (Oct6.2).

## 🟢 Resolved in Oct6.1
*   **Issue #AUDIT-1006-1: AlarmOverlayService State Leak Audit & Refinement.** Refactored `AlarmOverlayService` to initialize `SimpleUiStateProvider` and its associated flows at the service lifecycle level (`onCreate`). This prevents flow instability and potential memory leaks during service transitions. Seeded `sessionStateFlow` with an immediate value to eliminate initial black frames. (Oct6.1 - R-ID 288).
*   **Issue #AUDIT-1006-2: Background Service Transition Latency Audit.** Verified and optimized the transition to the background `AlarmOverlayService`. Ensured immediate emission of critical state to maintain safety-critical responsiveness after root-level UI invalidation removal. (Oct6.1).

... (Historical resolutions omitted for brevity)
