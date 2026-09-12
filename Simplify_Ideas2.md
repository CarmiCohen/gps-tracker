# Simplification Ideas 2

*   **Idea #7 (HUD State Logic Extraction)**: Move the logic for mapping `ConnectivitySuite` flows to HUD states into a dedicated `HudStateMapper` to reduce `MainViewModel` complexity.
*   **Idea #8 (Common Repository Base)**: Create a base class for `OfflineRepository` and `LogRepository` to share common serialization and pruning logic.
*   **Idea #13 (HUD Mapping Centralization)**: Consolidate HUD state construction logic. Currently, `UiStateAggregator` and `DashboardStateProvider` share responsibilities that could be unified into a single stateless mapper to prevent future arity issues during flow combination (R-ID 286).
*   **Idea #15 (Hardware Flag Abstraction)**: [RESOLVED Sep.12.00] Consolidated bitmask flags into type-safe `LedStatus` object (R-ID 264).
