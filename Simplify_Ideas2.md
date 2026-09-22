# Strategic Simplification Ideas (Sep.22.00)

## 🎯 Architecture & State Management
*   **Idea #250: Explicit Telemetry Nullability**: Instead of using `-1` as a magic value for uninitialized satellite counts, migrate `HudTelemetryState` to use nullable `Int?`. This would leverage Kotlin's type system to handle the "no data" state more safely in Compose.
*   **Idea #251: Peer Activity Event Stream**: Currently, `isPeerActive` is calculated in a 2-second pulse loop in `MainViewModel`. Consider exposing a dedicated `peerActivityFlow` from `RemoteStatusRepository` that calculates this state reactively based on incoming packet timestamps, reducing loop overhead in the ViewModel.
*   **Idea #252: StatusRowData Decomposition**: `StatusRowData` in `SharedUiComponents.kt` is becoming dense. Consider splitting it into smaller composables like `BatteryIndicator`, `EnvironmentIndicator`, and `SatelliteIndicator` to improve JIT performance and readability.
