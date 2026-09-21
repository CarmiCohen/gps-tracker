# Simplicity Audit & Architectural Refactoring Ideas (Sep.21.131)

## 🎯 Current Focus: HardwareSuite Pattern Convergence

### 1. GNSS Sampling Logic Consolidation
*   **Problem**: The calculation of GNSS sampling intervals (standard vs throttled) and the subsequent emission of detail updates is split between `gnssStatusCallback` and the auditor.
*   **Opportunity**: Encapsulate GNSS policy in a small `GnssPolicyEngine` or similar helper to decouple the hardware callback from the throttling and auditing rules. (Completed in Sep.21.128 via `GnssPolicyEngine`)

### 2. Forensic Auditing Code Pruning
*   **Problem**: Remnants of initial engineering exploration (`maxGnssJitterMs` property and `processReflection` function) remained in `HardwareSuite.kt`, creating clutter.
*   **Opportunity**: Prune these unreferenced tracking parameters to maximize readability and maintain zero architectural bloat. (Completed in Sep.21.130)

### 3. Unified Session Lifecycle Management
*   **Problem**: `HardwareSuite.resetBaseline`, `TrackerService.resetServiceTimers`, and `IntegrityMonitor.resetStats` all manually clear different parts of the transient state.
*   **Opportunity**: Centralize session state management in a `SessionLifecycleCoordinator`. This component would ensure that all hardware peaks, temporal lockouts, and vitality markers are zeroed atomically upon session restart, eliminating the risk of stale state leakage.

### 4. Fast-Path Configuration Convergence
*   **Problem**: Both Acoustic and Light fast-paths require manual baseline synchronization in the service tick to avoid divergence with the main validation engine.
*   **Opportunity**: Unify the fast-path implementation in `HardwareSuite` using a generic `HardwareFastPath<T>` delegate that automatically handles its own baseline decay or synchronization when the main engine updates.
