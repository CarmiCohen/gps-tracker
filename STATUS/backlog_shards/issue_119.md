# Issue #119: Boot Persistence Integrity

## Status: Resolved (July.22.02)
## Requirement: Activation Authority

### Description
The system's background lifecycle revival must be strictly controlled to avoid unwanted battery drain. Foreground services should only be restarted after a device boot or during periodic maintenance if the user has explicitly activated the system.

### Resolution
- **Master Switch Authority**: Hardened `BootServiceStartWorker` and `MaintenanceWorker` to perform a mandatory check on the `isSystemActive` flag in `DataStore` before initiating any service startup logic.
- **Worker Reliability**: Refactored the `WorkManager` enqueueing logic to ensure that if `isSystemActive` is false, all existing maintenance work is cancelled and no new work is scheduled.
- **Service Self-Termination**: Added a safety check in `TrackerService.onStartCommand` to self-stop if the activation flag is missing, protecting against edge-case race conditions.

### Verification
- [x] Verified that the app does NOT start services after a reboot if "Active" was toggled off.
- [x] Verified that services correctly revive after reboot if "Active" was toggled on.
