# Handover (Aug.27.05) - Hardware Lifecycle Hardening (Final)

## 🎯 Current Status
- **Goal**: Deterministic disposal of native hardware resources.
- **Status**: 🟢 **RESOLVED** (Concern #748: CallbackFlow BaseEventQueue Leak).
- **Version**: `Aug.27.05`
- **Database**: v73
- **Audit Baseline**: SOT: 164, Resolved: 748, Open: 46, Testing: 100 Chapters, 43 Sub-items, Simplification Ideas: 208, QA Status: 197.

## 🧬 Implementation Summary: Aug.27.05
- **Concern #748 Remediation**: **CallbackFlow Hardening**.
    - Identified that `hardwareObservationFlow` in `GpsManager.kt` was the primary source of the persistent `BaseEventQueue` warnings on A15 hardware.
    - **Synchronous awaitClose**: Updated `awaitClose { ... }` to use `Tasks.await()` for `removeLocationUpdates(fusedCallback)`.
    - **Interval Swap Protection**: Added synchronous removal to `flatMapLatest` within the flow to prevent overlapping native queues during polling interval changes.
    - **Revival Hardening**: Updated `restartLocationUpdates` to await the removal of the previous `revivalCallback`.
- **Architectural Update**: SOT Requirement 1.8 updated to mandate synchronous unregistration in all `callbackFlow` implementations.
- **Integrity**: Verified successful build `app:assembleDebug` and version bump to `Aug.27.05`.

## 🚀 Next Steps
- **Final Regression**: Deploy `Aug.27.05` and verify that role-swaps (Tracker -> Viewer) are now 100% silent in Logcat regarding `BaseEventQueue` disposal.
- **Abstraction**: Implement `ManagedLocationProvider` from `Simplify_Ideas2.md` to wrap Play Services Tasks.

vAug.27.05
