# Simplicity Audit & Architectural Clean-up Ideas (Sep.22.31)

Following the resolution of Issue #1182, here are evaluated ideas to further optimize code clarity and eliminate complexity:

1. **Acoustic and Light Sensor Baseline Decoupling**: Move high-frequency fast-path state evaluation entirely out of `LocationSentinel` and instead perform a unified, pre-filtered sensor data package submission. This keeps `LocationSentinel` strictly focused on spatial-temporal coordinate validation.
2. **Unified EvaluationSnapshot for Tick Loop**: Group remaining telemetry fields (health, battery, network) into a unified `EvaluationSnapshot` to allow single-pass atomic consumption within `TrackerService.processTick`. This would further reduce the parameter surface area between the service and the engine.
