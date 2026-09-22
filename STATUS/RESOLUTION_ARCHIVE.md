# Resolution Archive & Forensic Traceability Log

## 🟢 Resolved Issues & Refactoring Record

### Sep.22.05
*   **Issue #1181: UseCase Functional Consolidation**
    *   *Remediation*: Consolidated `HomePointUseCase` and `MapUseCase` into a single high-cohesion `SpatialLogicUseCase`, streamlining domain logic boundaries and reducing the ViewModel dependency injection surface area (R-ID 402).

### Sep.22.04
*   **Issue #1180: DataStore List Mutation Extension**
    *   *Remediation*: Implemented a generic `mutate` extension function for `DataStore<AppSettings>` to encapsulate atomic, race-free list and field updates. Refactored `SettingsRepository` to use this extension across all persistence methods, streamlining the data layer and eliminating redundant builder/update boilerplate (R-ID 401).

### Sep.22.03
*   **Issue #1179: Corrupted Home Point Addition Logic**
    *   *Remediation*: Implemented atomic `addHomePoint` and `removeHomePoint` methods in `SettingsRepository` using DataStore's `updateData` to prevent list corruption during rapid sequential updates. Refactored `MainViewModel` to persist the `ADD` geofence mode during batch operations and force `isFenceVisible` to `true` upon entry, ensuring immediate visual confirmation and a friction-less user experience (R-ID 400).

### Sep.22.00
*   **Issue #1178: Initial GNSS Satellite Count Blanking Prior to Initial Lock**
    *   *Remediation*: Set default satellite counts to -1 in `LocationUpdate` and `HudTelemetryState`. Updated `UiStateMapper` and `SharedUiComponents` to distinguish -1 (no data) from 0 (jammed/blocked) by displaying "--" until the first hardware fix is processed (R-ID 399).
*   **Issue #1177: Static Role Branding on Selection Screen Cards**
    *   *Remediation*: Integrated `isPeerActive` check into `MainViewModel` and passed it to `LandingScreen`. The Viewer card now dynamically dims when no telemetry is detected, improving role clarity during the initial handshake phase (R-ID 398).
*   **Issue #1176: Mismatched Temperature Unit Prefix Layout Ordering**
    *   *Remediation*: Corrected text component placement in `StatusRowData` within `SharedUiComponents.kt` to suffix the degree sign (`0°`) instead of prefixing it, ensuring alignment with SI standard presentation (R-ID 397).

### Sep.21.132
*   **Issue #1165: Unified Session Lifecycle Management**
    *   *Remediation*: Centralized the zeroing of hardware baseline parameters, temporal lockout registers, forensic latches, and vitality markers into `SessionLifecycleCoordinator`, ensuring atomic integrity upon tracking resets (R-ID 396).

### Sep.21.131
*   **Issue #1174: Interface Isolation Utilities**
    *   *Remediation*: Created `LocationProcessorListener` and `DefaultLocationProcessorListener` with no-op methods to prevent test breakages during interface expansion and stabilize regression testing (R-ID 395).

### Sep.21.130
*   **Issue #1159: Unused Forensic Auditing Dead Code Elimination**
    *   *Remediation*: Removed the unused `maxGnssJitterMs` property and obsolete `processReflection` function from `HardwareSuite.kt` to simplify code architecture and prune tracking leftovers.

### Sep.21.129
*   **Version Release Bump**: Bumped application and baseline versioning to `Sep.21.129` in full alignment with development milestones.

### Sep.21.128
*   **Issue #1158: GNSS Sampling Logic Consolidation**
    *   *Remediation*: Encapsulated GNSS sampling policy (standard vs throttled) and auditing triggers in a nested `GnssPolicyEngine` within `HardwareSuite.kt`. This decouples the hardware callback from throttling rules and ensures symmetric auditing of jitter across all performance tiers (R-ID 394).

### Sep.21.127
*   **Issue #1156: Unused Forensic Abstraction**
    *   *Remediation*: Refactored `HistoryManager.backfillAnalyticalGaps` to consume the specialized `EngineAcousticSample` sequence from `HardwareSuite.getAcousticSamples`, eliminating dead code and ensuring environmental noise is captured with semantic precision (R-ID 393).
*   **Issue #1157: Telemetry Abstraction Integration (Idea 5)**
    *   *Remediation*: Refactored `HistoryManager.fillRealGap` and `TelemetryAggregator` to consume `EngineAcousticSample` directly. This completes the decoupling of environmental noise (dB) from satellite SNR in the forensic ribbon history (R-ID 393).

### Sep.21.125
*   **Issue #1155: Acoustic-SNR Semantic Mismatch**
    *   *Remediation*: Introduced `EngineAcousticSample` and refactored `HardwareSuite.getAcousticSamples` to return a sequence of this new type (R-ID 393).
*   **Issue #1153: Forensic Sequence Race Condition**
    *   *Remediation*: Refactored `CircularStateBuffer.forensicSequence` to use a multi-pass custom Sequence implementation with locking (R-ID 392).
*   **Issue #1154: Forensic Allocation Spike**
    *   *Remediation*: Replaced `toList()` snapshot with a lazy, zero-allocation sequence iteration (R-ID 392).
