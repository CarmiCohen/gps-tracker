# Simplicity Audit & Architectural Refactoring Ideas (Sep.21.00)

## 🎯 Current Focus: HardwareSuite Pattern Convergence

### 1. HardwareSuite Snapshot Unification
*   **Problem**: `consumeLogicSnapshot` and `consumeForensicSnapshot` are nearly identical, differing only in which buffer they read and which peaks they reset.
*   **Opportunity**: Refactor into a single `internalConsumeSnapshot(buffer: CircularStateBuffer<ForensicSnapshot>, isForensic: Boolean)` method. This would reduce boilerplate and ensure that thread-safety improvements are always applied to both paths simultaneously.

### 2. Flyweight Sequence Abstraction
*   **Problem**: `getSnrSamples`, `getSensorSamples`, and `getAcousticSamples` all implement similar filtering/mapping logic with internal flyweight objects.
*   **Opportunity**: Create a generic utility in `CircularStateBuffer` or a helper extension to handle `Sequence` generation with a provided "reset/copy" lambda, reducing repetitive code in `HardwareSuite`.

### 3. GNSS Sampling Logic Consolidation
*   **Problem**: The calculation of GNSS sampling intervals (standard vs throttled) and the subsequent emission of detail updates is split between `gnssStatusCallback` and the auditor.
*   **Opportunity**: Encapsulate GNSS policy in a small `GnssPolicyEngine` or similar helper to decouple the hardware callback from the throttling and auditing rules.

### 4. ForensicAuditor Role Synchronization
*   **Problem**: `recordGnssStatus` updates all roles at once because jitter is a global hardware property, but `recordGpsFix` and `evaluateStability` are role-tagged.
*   **Opportunity**: Consider if role-specific jitter counters are necessary or if a unified "Hardware Health" state should be shared by roles to avoid redundant peak tracking in `RoleState`.

### 5. Telemetry Source Abstraction (#1121)
*   **Problem**: `ViewerService` manually extracts fields from `connectivitySuite.trackerStatus` for alarm evaluation, creating a maintenance burden if the model changes.
*   **Opportunity**: Implement a `TrackerStatus.toAlarmEvaluationParams()` or a dedicated `RemoteTelemetrySnapshot` class to encapsulate the mapping from network status to alarm inputs, ensuring consistent isolation between local hardware and remote telemetry.

### 6. Unified Session Lifecycle Management
*   **Problem**: `HardwareSuite.resetBaseline`, `TrackerService.resetServiceTimers`, and `IntegrityMonitor.resetStats` all manually clear different parts of the transient state.
*   **Opportunity**: Centralize session state management in a `SessionLifecycleCoordinator`. This component would ensure that all hardware peaks, temporal lockouts, and vitality markers are zeroed atomically upon session restart, eliminating the risk of stale state leakage.

### 7. Fast-Path Configuration Convergence
*   **Problem**: Both Acoustic and Light fast-paths require manual baseline synchronization in the service tick to avoid divergence with the main validation engine.
*   **Opportunity**: Unify the fast-path implementation in `HardwareSuite` using a generic `HardwareFastPath<T>` delegate that automatically handles its own baseline decay or synchronization when the main engine updates.
