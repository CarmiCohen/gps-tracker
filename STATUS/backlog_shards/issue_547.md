# Issue #547: Kernel Performance Warning (`userfaultfd`)

## 🎯 Status: Open (July.24.06)
**Category**: Performance / OS Compatibility

---

## 📝 Description
The system logs a kernel warning `userfaultfd: MOVE ioctl seems unsupported` on target hardware (Android 15 / Samsung A15). This feature is typically used by the ART garbage collector for Concurrent Mark Compact cycles.

## 🔍 Observations
- **Observation**: `userfaultfd: MOVE ioctl seems unsupported` observed in system logs.
- **Impact**: Potential performance degradation or increased jank during GC cycles if the kernel lacks support for efficient memory moving, which is critical for the new Android 15 GC performance optimizations.

## 🛠️ Planned Action
- Monitor GC duration and frequency in telemetry.
- Evaluate if high-frequency telemetry cycles (10Hz) are significantly impacted by GC pauses.
- Investigate if there are fallback memory management strategies for devices lacking this ioctl.

## 🔗 References
- **Requirement**: Performance Stability
- **Cycle**: July.24.06
