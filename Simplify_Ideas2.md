# Simplification & Architecture Ideas (Sep.02.72)

## Active Ideas
*   **Idea #240: ViewModel Decomposition**. As `MainViewModel` handles an increasing number of event types (Telemetry, Signaling, Navigation, Map), consider splitting it into feature-specific ViewModels (e.g., `MapViewModel`, `SessionViewModel`) to reduce cognitive load and JIT pressure on budget hardware (R-ID 247).
*   **Idea #239: Event-to-Command Mapping Automation**. Explore using a metadata-driven approach or a sealed class hierarchy visitor for `onEvent` to reduce the boilerplate of manual delegation to UseCases.
