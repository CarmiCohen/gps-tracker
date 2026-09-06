# Simplicity Audit & Future Simplification Ideas (Sep.06.17)

## 🎯 Current Audit: Issue #922
The introduction of `CircularStateBuffer` significantly simplified the internal state management of `HardwareProvider` by removing manual index wrapping logic (`(idx + 1) % size`). 

## 💡 Simplification Ideas
1.  **Generic Snapshotting**: The `ForensicSnapshot` class in `HardwareProvider` could be moved to `EngineModels` to allow better reuse in the upcoming `ForensicAuditor` extraction (Issue #922 Part B).
2.  **Unified Buffer Access**: Currently, `snrBuffer` and `sensorBuffer` have slightly different "sample" types. Standardizing these into a single forensic event wrapper would reduce the number of `asSequence()` iterations in `HistoryManager`.
3.  **Managed Sensor Unification**: The `ManagedSensorListener` base class could be extended to handle the `CircularStateBuffer` lifecycle automatically, further reducing boilerplate in `HardwareProvider`.
