# Issue #042: Identity Sanitization Visibility
**Status**: Resolved (v9.3.2)
**Priority**: Medium
**Requirement**: R976

## Description
The system must notify the user if their Tracker or Viewer ID is automatically sanitized or reset to default during a migration or due to illegal characters (Requirement R975).

## Resolution
- Implemented `identitySanitizedFlow` in `SettingsRepository`.
- Added a migration flag that triggers a UI notification (AlertDialog) when a sanitization event occurs.
- Verified that malformed IDs are reset to "T" or "V" prefixes without app crashes.
- Verified in v9.3.2; validation tracked in #067.
