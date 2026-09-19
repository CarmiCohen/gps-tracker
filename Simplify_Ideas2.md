# Simplicity Audit & Architectural Refactoring Ideas (Sep.19.09)

## 🎯 Current Focus: HardwareSuite Pattern Convergence

### 1. HardwareSuite Snapshot Unification
*   **Problem**: `consumeLogicSnapshot` and `consumeForensicSnapshot` are nearly identical.
*   **Opportunity**: Refactor into a single `internalConsumeSnapshot(buffer: CircularStateBuffer<ForensicSnapshot>, isForensic: Boolean)` method. This would reduce boilerplate and ensure that thread-safety improvements are always applied to both paths simultaneously.

### 2. Flyweight Sequence Abstraction
*   **Problem**: `getSnrSamples`, `getSensorSamples`, and `getAcousticSamples` all implement similar filtering/mapping logic with internal flyweight objects.
*   **Opportunity**: Create a generic utility in `CircularStateBuffer` or a helper extension to handle `Sequence` generation with a provided "reset/copy" lambda, reducing repetitive code in `HardwareSuite`.

### 3. Display Flickering State Consolidation
*   **Problem**: `isDisplayFlickering` (AtomicBoolean) and `lastDisplayTransitionRt` are tracked separately from other display states.
*   **Opportunity**: Move these into a `DisplayHealth` data class or similar structure to keep the root `HardwareSuite` namespace cleaner as more display-related forensic checks are added.
