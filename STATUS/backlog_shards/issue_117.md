# Issue #117: AlarmOverlay Callback Typos

## 🎯 Status: Resolved (Historical)
**Category**: UI / Bug Fix

---

## 📝 Description
The `AlarmOverlay` component had a typo in the `onGoToMap` callback assignment in `MainAppContent.kt`, which caused the map navigation to fail when dismissed from the emergency overlay.

## 🛠️ Resolution
- Corrected the callback mapping in `MainAppContent.kt`.
- Verified that "Go to Map" correctly dismisses the alarm state and centers the view on the violating coordinate.

## 🔗 References
- **File**: `app/src/main/java/com/gps19/app/MainAppContent.kt`
