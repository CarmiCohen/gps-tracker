# Code & Architecture Simplification Ideas (Aug.18.05)

Following the Urban Multipath Hardening (Issue #201), here are ideas to simplify the `core:engine` and overall app architecture:

### 1. Domain-Specific Constant Grouping
`EngineConstants.kt` has become a "kitchen sink" for all magic numbers. 
- **Idea**: Group related constants into nested `object` containers or separate files (e.g., `GeofenceConfig`, `MotionThresholds`, `AcousticParams`).
- **Benefit**: Improves IDE autocomplete discoverability and prevents "constant fatigue" when searching for specific logic gates.

### 2. Explicit State Machine for Tracking Status
Currently, the "Stationary" vs "Moving" state is determined by a mix of `stationaryProb` (EMA), `isPhysicallyStationary` (IMU), and various "Muzzle" or "Grace" flags spread across `LocationProcessor` and `LocationSentinel`.
- **Idea**: Implement an explicit `TrackingStateMachine` that handles transitions between `PARKING`, `MOVING`, `SUSPICIOUS`, and `JUMPING`.
- **Benefit**: Centralizes transition logic and makes it easier to unit test edge cases (like urban canyons) without side effects from unrelated sensor flags.

### 3. Sentinel Sub-Component Delegation
`LocationSentinel.kt` handles everything from GNSS filtering to Barometric lift and Acoustic spikes.
- **Idea**: Decompose `LocationSentinel` into specialized sub-sentinels (e.g., `EnvironmentalSentinel` for Lux/Baro/Temp, `KineticSentinel` for Vibration/Tilt/Shock).
- **Benefit**: Reduces file size and allows developers to work on specific sensor logic without navigating a 600+ line class.

### 4. Data-Driven Physical Tamper Validation
`checkPhysicalTamper` is a long chain of `if` statements.
- **Idea**: Use a "Security Policy" pattern where different sensor monitors register their violation conditions.
- **Benefit**: Simplifies adding new tamper sensors (e.g., future Wi-Fi/Cellular ID shifts) and makes the summary logic in `MainAlarmLogic` cleaner.

### 5. Unified "Forensic Pulse" Object
High-frequency forensics (100Hz) are manually gathered and passed through the `EngineConnectionPoint` flyweight.
- **Idea**: Create a unified `ForensicPulse` data structure that is automatically populated by sensors and the GNSS engine.
- **Benefit**: Reduces the manual mapping required in `TelemetryProcessor` and ensures all forensic fields are captured consistently across all layers.

### 6. Logic Optimization: Anchor Score vs Probability
We currently track both an `anchorEscapeScore` and a `stationaryProb`.
- **Idea**: Converge these into a single "Kinetic Certainty" index that factors in both GPS velocity and IMU stability.
- **Benefit**: Eliminates the "binary release" bug found in #201 and simplifies the decision logic in `AnchorEvaluator`.
