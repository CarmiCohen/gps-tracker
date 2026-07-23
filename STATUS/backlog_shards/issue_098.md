# Issue #098: Samsung Step Detector Stalling (R405c)

## Status: Open (Reopened July.24.01)
## Requirement: R405c, R107c, R107d

### Description
On budget Samsung hardware (A15), the Step Detector may fail to register because `ACTIVITY_RECOGNITION` is reported as missing by the engine even after being granted in the UI. This results in the "Stationary Hard-Lock" never being challenged.

### Resolution (In Progress)
- **Permission Immediacy (R107c)**: Hardened `SystemStatusProviderImpl` to perform synchronous refreshes when forced, ensuring the UI doesn't show stale "denied" states.
- **Reactive Sync (R107d)**: `MainViewModel` now detects permission grant transitions and immediately triggers a sensor re-registration in the service.
- **Stay-Alive Fallback**: If the detector remains unregistered, the system engages an Accelerometer fallback pulse to maintain process priority.

### Verification
- [x] Verified synchronous refresh clears UI alert immediately after grant.
- [x] Logcat confirms "Triggering reactive sensor sync" upon permission change.
- [ ] Pending: Verify Step Detector successfully initializes on A15 without requiring the 5-minute recovery loop.
