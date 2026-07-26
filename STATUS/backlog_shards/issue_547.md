# Issue #547: Kernel Performance Warning (`userfaultfd`)

## 🎯 Status: Resolved (July.25.13)
**Category**: Performance / OS Compatibility

---

## 📝 Description
The system logs a kernel warning `userfaultfd: MOVE ioctl seems unsupported` on target hardware (Android 15 / Samsung A15). This feature is typically used by the ART garbage collector for Concurrent Mark Compact cycles.

## 🔍 Observations
- **Observation**: `userfaultfd: MOVE ioctl seems unsupported` observed in system logs.
- **Impact**: Potential performance degradation or increased jank during GC cycles if the kernel lacks support for efficient memory moving.

## 🛠️ Root-Cause Mitigation (July.25.07 - July.25.13)
- **Zero-Churn Buffers (R547b)**: Refactored high-frequency engine components (`GtoEngine` and `LocationProcessor`) to use primitive circular buffers (`DoubleArray`, `LongArray`) for kinematic windows and accuracy tracking. This eliminates object allocations in the 1Hz-10Hz path.
- **UI State Decomposition (R547)**: Decomposed monolithic UI state into persistent and transient streams to minimize heap pressure.
- **Kernel Jitter Monitoring (R547d)**: Integrated `LatencyMonitor` into the `dashboardState` pipeline in `MainViewModel`. Added a 30ms jitter probe specifically for A15 hardware to detect and log forensic warnings when ART compaction stalls occur.

## 🔗 References
- **Requirement**: Performance Stability (R547b), Kernel Jitter Monitoring (R547d)
- **Cycle**: July.25.13
