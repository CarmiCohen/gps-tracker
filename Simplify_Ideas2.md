# Simplicity Audit & Architectural Clean-up Ideas (Sep.22.32)

Following the resolution of Issue #1162, here are evaluated ideas to further optimize code clarity and eliminate complexity:

1. **Acoustic and Light Sensor Baseline Decoupling**: Move high-frequency fast-path state evaluation entirely out of `LocationSentinel` and instead perform a unified, pre-filtered sensor data package submission. This keeps `LocationSentinel` strictly focused on spatial-temporal coordinate validation.
2. **Unified MonitorService Transition**: Merge `TrackerService` and `ViewerService` into a single `MonitorService` that reactively changes behavior based on the active `appMode`, eliminating redundant boilerplate and FGS management.
