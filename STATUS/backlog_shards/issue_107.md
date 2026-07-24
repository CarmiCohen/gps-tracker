# Issue #107: Activity Recognition Permission Hardening

## 🎯 Status: Resolved (Historical)
**Category**: Permissions / Android 10+

---

## 📝 Description
Android 10 (API 29) introduced the requirement for the `ACTIVITY_RECOGNITION` permission to access physical sensors like the Step Detector. The app was failing to register these sensors on modern devices because the permission wasn't part of the core flow.

## 🛠️ Resolution
- Added `android.permission.ACTIVITY_RECOGNITION` to the `AndroidManifest.xml`.
- Integrated the permission check into `MainAppContent` and `AppSensorManager`.
- Ensured hardware registration is deferred until the permission is explicitly granted by the user.

## 🔗 References
- **Requirement**: R107 (Permission Immediacy)
- **Files**: `MainAppContent.kt`, `AppSensorManager.kt`, `MainUiState.kt`
