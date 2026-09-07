# Simplification Ideas (vSep.07.80)

1.  **Forensic Auditor Consolidation (Priority)**: Unify the duplicate GNSS jitter and stability audit logic between `TrackerService` and `ViewerService` by fully utilizing the `ForensicAuditor` singleton for both roles.
2.  **Telemetry Data Class Flattening**: The `LocationUpdate` class contains over 60 fields. Consider splitting this into `KineticState`, `AtmosphericState`, and `IntegrityState` to reduce the overhead of partial updates.
3.  **Alarm Evaluation Logic**: The `evaluateAlarms` method in `AlarmManager` has reached high cyclomatic complexity. Logic can be partitioned into specialized evaluators (e.g., `PhysicalTamperEvaluator`, `HardwareHealthEvaluator`).
