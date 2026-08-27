# Handover (Aug.27.01) - Lifecycle Hardening (Managed Callbacks)

## 🎯 Current Status
- **Goal**: Final remediation of native resource leaks during hardware manager disposal.
- **Status**: 🟢 **RESOLVED** (Concern #742 & #744: Managed Hardware Callbacks & EventQueue Leak).
- **Version**: `Aug.27.01`
- **Database**: v73
- **Audit Baseline**: SOT: 21, Resolved: 744, Open: 47, Testing: 100 Chapters, 43 Sub-items, Simplification Ideas: 202, QA Status: 196.

## 🧬 Implementation Summary: Aug.27.01
- **Concern #744 Remediation**: **Persistent EventQueue Leak**.
    - Identified that `GpsManager.hardwareObservationFlow` used an anonymous `LocationCallback` within a `callbackFlow`. Due to `SharingStarted.WhileSubscribed(5000)`, the callback remained registered with the system for 5 seconds after the `GpsManager.stop()` command was issued.
    - Since `gpsThread` and its `Looper` were quit synchronously in `stop()`, the system could not dispose of the native event queue when the flow finally attempted unregistration, leading to the "BaseEventQueue.dispose" warning.
    - Implemented `activeLocationCallback` tracking in `GpsManager` with explicit `removeLocationUpdates` in the synchronized `stop()` block, ensuring cleanup occurs *before* the thread is destroyed.
- **Architectural Update**: Refined SOT Requirement 1.8 to mandate that all primary hardware callbacks must be explicitly tracked and synchronously unregistered, overriding transient flow-based management during destruction.
- **Version Incremented**: Updated `app/build.gradle` to `Aug.27.01`.
- **Integrity**: Verified successful build via `app:assembleDebug`.

## 🚀 Next Steps
- **Hardware Regression**: Perform role-swaps (Tracker -> Viewer -> Tracker) on A15 hardware. Confirm that the `BaseEventQueue` disposal warning no longer appears.
- **Simplicity Audit**: Review `Simplify_Ideas2.md` for the proposed `HardwareFlow` wrapper to automate this pattern and prevent future regressions.

vAug.27.01
