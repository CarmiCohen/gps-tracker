# Simplicity Audit & Architectural Refactoring Ideas (Sep.21.125)

## 🎯 Current Focus: HardwareSuite Pattern Convergence

### 1. GNSS Sampling Logic Consolidation
*   **Problem**: The calculation of GNSS sampling intervals (standard vs throttled) and the subsequent emission of detail updates is split between `gnssStatusCallback` and the auditor.
*   **Opportunity**: Encapsulate GNSS policy in a small `GnssPolicyEngine` or similar helper to decouple the hardware callback from the throttling and auditing rules.

### 2. ForensicAuditor Role Synchronization
*   **Problem**: `recordGnssStatus` updates all roles at once because jitter is a global hardware property, but `recordGpsFix` and `evaluateStability` are role-tagged.
*   **Opportunity**: Consider if role-specific jitter counters are necessary or if a unified "Hardware Health" state should be shared by roles to avoid redundant peak tracking in `RoleState`.

### 3. Unified Session Lifecycle Management
*   **Problem**: `HardwareSuite.resetBaseline`, `TrackerService.resetServiceTimers`, and `IntegrityMonitor.resetStats` all manually clear different parts of the transient state.
*   **Opportunity**: Centralize session state management in a `SessionLifecycleCoordinator`. This component would ensure that all hardware peaks, temporal lockouts, and vitality markers are zeroed atomically upon session restart, eliminating the risk of stale state leakage.

### 4. Fast-Path Configuration Convergence
*   **Problem**: Both Acoustic and Light fast-paths require manual baseline synchronization in the service tick to avoid divergence with the main validation engine.
*   **Opportunity**: Unify the fast-path implementation in `HardwareSuite` using a generic `HardwareFastPath<T>` delegate that automatically handles its own baseline decay or synchronization when the main engine updates.

### 5. Telemetry Abstraction Cleanup
*   **Problem**: `HardwareSuite.getAcousticSamples` returns a specialized `EngineAcousticSample` sequence, but `HistoryManager` is not yet updated to consume it, leaving the semantic decoupling incomplete in the telemetry ribbon.
*   **Opportunity**: Refactor `HistoryManager.backfillAnalyticalGaps` to consume acoustic samples directly, ensuring the forensic ribbon accurately reflects environmental noise without SNR ambiguity.
