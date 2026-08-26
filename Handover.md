# Handover (Aug.26.18) - Lifecycle Hardening

## 🎯 Current Status
- **Goal**: Resolve native resource leaks during hardware manager disposal.
- **Status**: 🟢 **RESOLVED** (Concern #742: Recurrent EventQueue Leak).
- **Version**: `Aug.26.18`
- **Database**: v73
- **Audit Baseline**: SOT: 181, Resolved: 742, Open: 47, Testing: 100 Chapters, 43 Sub-items, Simplification Ideas: 198, QA Status: 196.

## 🧬 Implementation Summary: Aug.26.18
- **Concern #742 Resolved**: **Recurrent EventQueue Leak**.
    - Identified that `GpsManager` was redundantly registering `GnssStatus.Callback` within its `callbackFlow`, leading to overlapping native event queues during polling interval changes.
    - Decoupled the GNSS callback lifecycle from the flow and tied it strictly to the synchronized `start()`/`stop()` lifecycle of the `GpsManager` singleton.
    - Hardened `AppSensorManager` step-detector registration to ensure it respects the `lifecycleLock` across async boundaries.
    - Verified that `BaseMonitorService` triggers synchronous cleanup of both managers in `onDestroy()`.
- **Architectural Update**: Refined SOT Requirement 1.8 to mandate persistence of hardware callbacks at the manager level rather than transient flows.
- **Version Incremented**: Updated `app/build.gradle` to `Aug.26.18`.

## 🚀 Next Steps
- **Regression Test**: Perform role-swaps (Tracker -> Viewer -> Tracker) and monitor logcat for the "A resource failed to call BaseEventQueue.dispose" warning. It should now be silenced.
- **Thermal Audit**: Monitor `AppSensorManager` high-load scaling during forensic stress tests on A15 hardware.

vAug.26.18
