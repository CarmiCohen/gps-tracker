# Issue #098: Reactive Sensor Sync & Permission Hardening

## 🎯 Status: Resolved (July.24.03)
**Category**: Hardware / Permissions

---

## 📝 Description
Transitions of critical permissions (specifically `ACTIVITY_RECOGNITION` on Android 10+) were not being reactively handled, requiring a service restart to engage hardware listeners.

## 🛠️ Resolution
- Implemented **Reactive Capability Sync**: The system now listens for permission grant events and triggers a synchronous hardware refresh.
- Added aggressive re-registration for the `Step Detector` to bypass OS permission propagation lag.
- Standardized `HardwareCapabilities` to include manual override states for Xiaomi devices.

## 🔗 References
- **Requirement**: R107d, R107e
- **Files**: `ViewerService.kt`, `AppSensorManager.kt`, `AppNotificationManager.kt`
