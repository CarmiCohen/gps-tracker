# 🏛️ Resolution Archive - Oct.3.2

## 🏁 Issue #1420-S / SOT ID 603: HUD Stabilization & Build Recovery
*   **Resolved**: Oct.3.2
*   **Root Cause**: The granular interface slicing introduced in Issue #1420 caused multiple compilation failures across the UI layer due to naming mismatches (`trackerLocPendingReason` vs `locationPendingReason`) and deprecated method signatures in `AlarmOverlay`.
*   **Remediations**:
    *   **Interface Compliance**: Updated `SystemHealthState` to explicitly implement the `Locatable` interface.
    *   **UI Alignment**: Synchronized `MainViewModel.kt`, `UiStateCoordinator.kt`, and `SharedUiComponents.kt` to use the standardized `locationPendingReason` property.
    *   **Call Site Refactor**: Updated `AlarmOverlay` consumers in `AlarmActivity` and `MainAppContent` to pass the `locatable` health slice instead of raw booleans.
    *   **Build Recovery**: Verified full project compilation via `:app:assembleDebug`.
*   **Significance**: High (Build Integrity).
*   **SOT ID**: 603

---

# 🏛️ Resolution Archive - Oct.3.1

## 🏁 Issue #1420 / SOT ID 601: Granular HUD Binding
*   **Resolved**: Oct.3.1
*   **Root Cause**: UI components and services were directly dependent on the monolithic `LocationUpdate` object, causing unnecessary coupling and redundant recompositions whenever any field in the monolith changed.
*   **Remediations**:
    *   **Interface Slicing**: Introduced `Locatable`, `BatteryProvider`, and `DeviceIdentity` interfaces in `core:engine`.
    *   **Implementation**: Updated `LocationUpdate` to implement these granular interfaces.
    *   **UI Refactoring**: Refactored `AlarmOverlay` and HUD state models to consume these interfaces.
    *   **Service Optimization**: Updated `AlarmOverlayService` to collect and pass only the required slice to the overlay.
*   **Significance**: Medium (Decoupling).
*   **SOT ID**: 601

---

## 🏁 Issue #SIMP-1510-1 / SOT ID 602: Native Stationary Convergence
*   **Resolved**: Oct.3.1
*   **Root Cause**: Performing high-frequency stationary detection and vibration floor EMA math in the JVM hot-path introduced overhead.
*   **Remediations**:
    *   **Native Offloading**: Expanded `JdHardwareManager` and native bridge (`n12`/`n13`) to handle stationary logic in C++.
    *   **Validator Integration**: Refactored `SentinelValidator` to utilize a `NativeFastPathProvider` delegate.
    *   **Fallback Integrity**: Maintained JVM implementation as a fallback for non-supported hardware.
*   **Significance**: Medium (Performance).
*   **SOT ID**: 602

---

# 🏛️ Resolution Archive - Oct.2.15

## 🏁 Issue #1290 / SOT ID 600: UI State Mapper Consolidation
*   **Resolved**: Oct.2.15
*   **Root Cause**: The `UiStateCoordinator` (or `UiStateMapper`) introduced an unnecessary layer of indirection for activity-scoped state projection, increasing dependency complexity without providing multi-consumer utility.
*   **Remediations**:
    *   **Logic Merging**: Transferred all mapping functions (`mapDashboardConnectivity`, `mapDashboardTelemetry`, `mapDashboardHealth`, `mapHudConnectivity`, `mapHudTelemetry`, `mapHudHealth`, `mapMapViewState`, `computeTrailSegments`) into `MainViewModel`.
    *   **State Migration**: Moved coordinate smoothing variables (`sTrkLat`, `sTrkLng`, etc.) to private properties within `MainViewModel`.
    *   **Dependency Pruning**: Removed `UiStateCoordinator` from the Hilt injection graph. Note: File remains on disk as dead code due to environment limitations.
*   **Significance**: Medium (Maintainability).
*   **SOT ID**: 600

---

## 🏁 Issue #1176 / SOT ID 599: Native FastPath Transitions
*   **Resolved**: Oct.2.15
*   **Root Cause**: High-frequency sensor spike detection (Acoustic and Light) in the JVM layer introduced significant event processing latency and GC pressure during sustained monitoring (250Hz+).
*   **Remediations**:
    *   **JNI Offloading**: Migrated the `HardwareFastPath` evaluation logic to native code (`n10`/`n11` in `jdHardware`).
    *   **JVM Fallback**: Maintained a graceful fallback to JVM-based spike detection in `HardwareSuite` if the native library is unavailable.
    *   **State Synchronization**: Implemented `updateFastPathConfig` to sync baseline, thresholds, and debounce parameters with the native layer.
    *   **Lifecycle Integrity**: Ensured native trackers are reset during suite teardown and baseline resets.
*   **Significance**: Low (Performance Optimization).
*   **SOT ID**: 599

---

# 🏛️ Resolution Archive - Oct.2.9

## 🏁 Issue #1314 / SOT ID 598: TrackerStatus Convergence
*   **Resolved**: Oct.2.9
*   **Root Cause**: Redundant telemetry DTOs (`TrackerStatus` and `LocationUpdate`) created mapping overhead, synchronization risks, and increased memory pressure due to constant object allocation in the hot-path.
*   **Remediations**:
    *   **Monolith Consolidation**: Merged all `TrackerStatus` fields into the `LocationUpdate` monolith in `core:engine`.
    *   **Mutability Restoration**: Implemented `var` convenience properties in `LocationUpdate` with custom setters delegating to partitioned internal state objects (`KineticState`, `AtmosphericState`, `IntegrityState`).
    *   **DTO Purge**: Completely removed the `TrackerStatus` class from `Models.kt`.
    *   **Mapping Alignment**: Updated `TelemetryMapper`, `MonitorService`, `ConnectivitySuite`, and `SettingsMapper` to consume the unified monolith.
    *   **Persistence Hardening**: Aligned `TelemetryProtobufMapper` with the new structure to ensure atomic persistence of the unified state.
*   **Significance**: High (Architecture/Performance).
*   **SOT ID**: 598

---

# 🏛️ Resolution Archive - Oct.2.8

## 🏁 Issue #1330 / SOT ID 597: Snap-to-Update Monolith
*   **Resolved**: Oct.2.8
*   **Root Cause**: The existence of a bridge layer between `SystemEvaluationSnapshot` (engine-internal) and `LocationUpdate` (persistence/signaling) introduced redundant mapping overhead and object churn.
*   **Remediations**:
    *   **DTO Consolidation**: Merged all engine evaluation fields into a unified `LocationUpdate` DTO.
    *   **Architecture Simplification**: Removed the `SystemEvaluationSnapshot` class entirely.
    *   **Zero-Allocation Mapping**: Eliminated the `mapSnapshotToUpdate` layer, as the engine now operates directly on the persistence-ready DTO.
    *   **Logic Refactoring**: Updated `LocationSentinel`, `LocationProcessor`, and `AppAlarmManager` to use the monolithic structure.
*   **Significance**: Medium (Architecture).
*   **SOT ID**: 597

---

# 🏛️ Resolution Archive - Oct.2.7

## 🏁 Issue #1329 / SOT ID 596: Telemetry Mapping Convergence
*   **Resolved**: Oct.2.7
*   **Root Cause**: Redundant and manual field injection/mapping logic was scattered across `AppEventCoordinator`, leading to potential inconsistencies and high churn in the domain orchestration layer.
*   **Remediations**:
    *   **Consolidated Mapping Authority**: Introduced `TelemetryMapper.mapTickToOutputs` to handle all DTO preparation (LocationUpdate, TrackerStatus) from engine tick events.
    *   **Centralized Injection**: Moved global alarm state injection (lastAlarmAckTs, violationStartTs) from the coordinator to the mapper.
    *   **Orchestration Simplification**: Reduced `AppEventCoordinator.handleTickEvaluated` to a clean sequence of Map -> Persist -> Signal.
*   **Significance**: High (Architecture).
*   **SOT ID**: 596

---

# 🏛️ Resolution Archive - Oct.2.6

## 🏁 Issue #1175 / SOT ID 595: Real-time Only Path (Strategic Removal of Forensic Backfilling)
*   **Resolved**: Oct.2.6
*   **Root Cause**: Legacy telemetry backfilling logic introduced significant architectural complexity, required large memory buffers (`backfillPool`, `backfillBuffer`), and increased synchronization risks during jitter-heavy connectivity transitions.
*   **Remediations**:
    *   **Architecture Simplification**: Purged all gap-filling and forensic backfilling logic from `HistoryManager`.
    *   **Memory Footprint Reduction**: Eliminated 1000-point backfill pools and supporting data sequences, reducing heap pressure on budget hardware (A15).
    *   **Dependency Pruning**: Removed `HardwareSuite` and `LocationProcessor` dependencies from `HistoryManager`, as history recording is now strictly event-driven.
    *   **Interface Hardening**: Removed plural `addHistoryPoints` from `MainRepository` to enforce single-point real-time ingestion.
*   **Significance**: Strategic (Simplification).
*   **SOT ID**: 595

---

# 🏛️ Resolution Archive - Oct.2.5

## 🏁 Issue #SIMP-1416-1 / SOT ID 594: Native Sensor Pulse Audit
*   **Resolved**: Oct.2.5
*   **Root Cause**: High-frequency sensor auditing (250Hz) in `ForensicAuditor.kt` caused significant JVM heap churn due to per-event object creation and synchronization overhead.
*   **Remediations**:
    *   **Native Pulse Tracking**: Offloaded pulse counting and frequency calculation to JNI (`jdhardware-jni.cpp`) using atomic-style global state.
    *   **JNI Fast Path**: Implemented `recordSensorPulse` in `JdHardwareManager` as a direct external call to minimize transition overhead.
    *   **Audit Delegation**: Refactored `ForensicAuditor` to query native Hz instead of tracking local counters.
*   **Significance**: Medium (Hardening).
*   **SOT ID**: 594

---

# 🏛️ Resolution Archive - Oct.2.3

## 🏁 Issue #1402-B / R-ID 582: WindowManager Lifecycle Hardening
*   **Resolved**: Oct.2.3
*   **Root Cause**: `AlarmOverlayService` manually implemented `LifecycleOwner` but lacked `ON_RESUME`/`ON_PAUSE` transitions. This potentially caused unpredictable behavior in Compose state flows and resource retention during long-duration alerts on A15 hardware.
*   **Remediations**:
    *   **Lifecycle Parity**: Added full state transitions to `showOverlay` and `onDestroy`.
    *   **Hardened Cleanup**: Explicitly call `disposeComposition()` before `removeViewImmediate()` to ensure the Compose tree is pruned before the window is destroyed.
*   **Significance**: High (Resource Integrity).
*   **SOT ID**: 582

---

# 🏛️ Resolution Archive - Oct.2.2

## 🏁 Issue #1417: Jitter-Resistant Connectivity Transitions
*   **Resolved**: Oct.2.2
*   **Root Cause**: Transient relay lag (500ms jitter) caused rapid oscillation between `RELAY_OFFLINE` and `SIGNAL_LOSS` states.
*   **Remediations**:
    *   **Temporal Hysteresis**: Added a 3s delay before triggering `RELAY_OFFLINE`.
    *   **Error Suppression**: Peer errors are now suppressed if the relay was offline within the last 3s window.
*   **Significance**: High (Alert Stability).
*   **SOT ID**: 593

## 🏁 Issue #1416: Memory Pressure Mitigation
*   **Resolved**: Oct.2.2
*   **Root Cause**: High-frequency sensor audits (250Hz) and sustained alerts on budget hardware (A15) caused cumulative heap growth.
*   **Remediations**:
    *   **Heap Probes**: Integrated real-time heap monitoring into `IntegrityMonitor`.
    *   **Reactive Throttling**: Forensic sampling interval is now dynamically throttled (up to 4x) when memory pressure is HIGH or CRITICAL.
    *   **Aggressive GC**: Triggered manual garbage collection and `historyManager.trimMemory()` at 200MB/250MB thresholds.
*   **Significance**: High (Resource Management).
*   **SOT ID**: 592

## 🏁 Issue #1415: CPU-Load Compensation for Sensors
*   **Resolved**: Oct.2.2
*   **Root Cause**: 100% CPU saturation during stress tests on the A15 caused the accelerometer to report erratic \"ghost\" vibration spikes.
*   **Remediations**:
    *   **Load Gating**: Integrated `cpuLoad` into `SentinelValidator`.
    *   **Hysteresis Expansion**: IMU evaluation thresholds are now expanded by 1.5x when CPU load exceeds 0.85.
    *   **Calibration Lock**: `Passive Zeroing` pauses during load bursts.
*   **Significance**: High (False Positive Mitigation).
*   **SOT ID**: 590, 591
