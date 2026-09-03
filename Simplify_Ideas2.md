# Simplification & Architecture Ideas (Sep.03.110)

## Active Ideas
*   **Idea #240: ViewModel Decomposition**. As `MainViewModel` handles an increasing number of event types (Telemetry, Signaling, Navigation, Map), consider splitting it into feature-specific ViewModels (e.g., `MapViewModel`, `SessionViewModel`) to reduce cognitive load and JIT pressure on budget hardware (R-ID 247).
*   **Idea #239: Event-to-Command Mapping Automation**. Explore using a metadata-driven approach or a sealed class hierarchy visitor for `onEvent` to reduce the boilerplate of manual delegation to UseCases.
*   **Idea #242: ForensicGraceManager**. Centralize all timing-based suppressions (GPS Warmup, Relay recovery, Hardware-specific signal grace) into a unified domain service. This would simplify `MainAlarmLogic.detectViolations` by replacing manual uptime/recovery checks with a single `isSuppressed(ViolationType)` call (Issue #247).
*   **Idea #244: Connectivity Lifecycle Observer**. The "poke" logic and heartbeat checks for A15 devices are currently scattered between `TrackerService` and `ConnectivitySuite`. Abstracting these into a `Lifecycle-aware ConnectivityWatchdog` would improve testability and reduce service-layer complexity.
