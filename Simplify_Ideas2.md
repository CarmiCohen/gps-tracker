# Simplification Ideas 2

*   **Idea #7 (HUD State Logic Extraction)**: Move the logic for mapping `ConnectivitySuite` flows to HUD states into a dedicated `HudStateMapper` to reduce `MainViewModel` complexity.
*   **Idea #8 (Common Repository Base)**: Create a base class for `OfflineRepository` and `LogRepository` to share common serialization and pruning logic.
*   **Idea #13 (HUD Mapping Centralization)**: [RESOLVED Sep.12.02] Consolidated HUD state construction logic into a single stateless `UiStateMapper` (R-ID 286).
*   **Idea #15 (Hardware Flag Abstraction)**: [RESOLVED Sep.12.00] Consolidated bitmask flags into type-safe `LedStatus` object (R-ID 264).
