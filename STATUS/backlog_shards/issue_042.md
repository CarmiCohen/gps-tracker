# Issue #042: Sanitization Visibility Hardening

## 🎯 Status: Resolved (Historical)
**Category**: UI / UX

---

## 📝 Description
The identity sanitization overlay was intermittently failing to appear or was being prematurely dismissed, leading to cases where default IDs (T/V) were used without user confirmation.

## 🛠️ Resolution
- Implemented a dedicated `identitySanitizedFlow` in `MainRepository` and `SettingsRepository`.
- Added an explicit `UiEvent.DismissIdentitySanitization` to ensure state-backed dismissal.
- Hardened the `AlertDialog` logic in `MainAppContent.kt` to persist until explicitly acknowledged.

## 🔗 References
- **File**: `app/src/main/res/values/strings.xml`, `MainAppContent.kt`
