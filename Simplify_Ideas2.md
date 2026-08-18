# Simplify Ideas - Aug.18.06

Ideas to reduce complexity and improve maintainability of the GPS Tracker project.

## 1. Domain-Specific Constant Grouping
`EngineConstants.kt` has become a "kitchen sink" for all magic numbers. 
- **Idea**: Group related constants into nested `object` containers or separate files (e.g., `GeofenceConfig`, `MotionThresholds`, `AcousticParams`).
- **Benefit**: Improves IDE autocomplete discoverability and prevents "constant fatigue".

## 2. Explicit State Machine for Tracking Status
Currently, the "Stationary" vs "Moving" state is determined by a mix of `stationaryProb`, `isPhysicallyStationary`, and various flags.
- **Idea**: Implement an explicit `TrackingStateMachine` that handles transitions between `PARKING`, `MOVING`, `SUSPICIOUS`, and `JUMPING`.

## 3. Sentinel Sub-Component Delegation
`LocationSentinel.kt` handles everything from GNSS filtering to Barometric lift.
- **Idea**: Decompose `LocationSentinel` into specialized sub-sentinels (e.g., `EnvironmentalSentinel`, `KineticSentinel`).

## 4. Unified Logging Data Path (New - #202)
Currently, the app uses `LogEntry` for domain logic and `LogEntity` for Room persistence. 
- **Idea**: Standardize a "LogRecord" interface that both classes implement, or use `LogEntity` directly in the domain layer to eliminate the mapping layer entirely for all log types.

## 5. Forensic Codec Decoupling (New - #202)
The memory-mapping logic in `ForensicSpillBuffer` is coupled with the binary format.
- **Idea**: Extract a `ForensicCodec` or `ForensicSerializer` class to allow changing binary formats without touching buffer management logic.

## 6. Logic Optimization: Anchor Score vs Probability
- **Idea**: Converge `anchorEscapeScore` and `stationaryProb` into a single "Kinetic Certainty" index that factors in both GPS velocity and IMU stability.
- **Benefit**: Eliminates the "binary release" logic and simplifies the decision logic in `AnchorEvaluator`.
