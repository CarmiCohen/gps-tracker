# Issue #547: Kernel Performance Warning (`userfaultfd`)

## 🎯 Status: Resolved (July.25.07)
**Category**: Performance / OS Compatibility

---

## 📝 Description
The system logs a kernel warning `userfaultfd: MOVE ioctl seems unsupported` on target hardware (Android 15 / Samsung A15). This feature is typically used by the ART garbage collector for Concurrent Mark Compact cycles.

## 🔍 Observations
- **Observation**: `userfaultfd: MOVE ioctl seems unsupported` observed in system logs.
- **Impact**: Potential performance degradation or increased jank during GC cycles if the kernel lacks support for efficient memory moving.

## 🛠️ Root-Cause Mitigation (July.25.07)
- **Action**: Refactored high-frequency engine components (`GtoEngine` and `LocationProcessor`) to use primitive circular buffers (`DoubleArray`, `LongArray`) for kinematic windows and accuracy tracking. 
- **Result**: Eliminated transient object allocations and boxing churn in the 1Hz-10Hz tick path. This achieves "Zero-Churn" in the coordinate processing pipeline, effectively bypassing the need for kernel-level memory moving by preventing the heap pressure that triggers compaction.

## 🔗 References
- **Requirement**: Performance Stability (R547b)
- **Cycle**: July.25.07
