# Issue #107: Step Detector Permission Hardening (R107)

## Status: Resolved (July.20.07)
## Requirement: R107

### Description
On Android 10 (API 29) and above, the hardware Step Detector requires the `android.permission.ACTIVITY_RECOGNITION` permission. Without it, sensor registration fails silently, leading to gaps in forensic SIT detection.

### Resolution
- **Permission Lifecycle**: Implemented a mandatory permission check in `AppSensorManager` before attempting to register the `TYPE_STEP_DETECTOR` listener.
- **Health Checks**: Added a diagnostic event to the log if the permission is missing, and integrated a prompt into the `PhoneSetupOverlay`.
- **Graceful Fallback**: If the Step Detector is unavailable or permission is denied, the system automatically engages the Accelerometer-based "Stay-Alive" pulse (Issue #098).

### Verification
- [x] Verified that SIT detection resumes immediately after granting the permission on Android 14.
- [x] Confirmed the diagnostic warning appears when the permission is revoked.
