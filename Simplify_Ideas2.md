# Simplification Ideas (vSep.08.20)

1.  **Telemetry Data Class Flattening (Priority)**: `LocationUpdate` now exceeds 70 fields. Partition it into specialized, lean states: `KineticState` (Position/Speed), `AtmosphericState` (Baro/Temp/Lux), and `IntegrityState` (Errors/Throttling). This will reduce allocation churn and improve serialization performance on budget hardware (R-ID 238).
2.  **Alarm Evaluation Logic partitioning**: Partition the monolithic `evaluateAlarms` method in `AlarmManager` into specialized evaluators (e.g., `PhysicalTamperEvaluator`, `HardwareHealthEvaluator`) to reduce cyclomatic complexity and improve unit test coverage.
3.  **UiStateAggregator Refactoring**: As UI flags increase, the aggregator is becoming complex. Transition to a "Plugin" architecture where different modules contribute to the final UI state independently.
4.  **Telemetry Mapper Unification**: Once the model flattening (Idea #1) is stable, refactor `TelemetryMapper` to map entire state sub-objects (Kinetic, Atmospheric, Integrity) instead of individual primitive fields. This will reduce boilerplate and the risk of "mapping gaps" in forensic data (Sep.08.20).
