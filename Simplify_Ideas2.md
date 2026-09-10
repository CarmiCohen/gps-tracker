# Simplification Ideas (Sep.10.30)

## 🏗️ Architectural Simplifications
*   **Vitality Tracking Unification (Idea #4)**: Currently, `IntegrityMonitor.kt` tracks `lastStorageUpdateRt`, `lastPowerUpdateRt`, etc., as individual variables. These could be unified into a `Map<VitalitySource, Long>` or a single `SharedFlow<VitalityHeartbeat>` to simplify the heartbeat auditing logic and reduce boilerplate when adding new monitored components.
*   **Watchdog Danger Window Abstraction**: The "danger window" check in `SystemMonitor` is now hardened but adds complexity to the grid calculation. Moving the grid alignment logic to a dedicated `GridScheduler` utility would clean up the `SystemMonitor` service wrapper.
