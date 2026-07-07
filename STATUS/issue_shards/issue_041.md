# Issue #041: Identity Sanitization Hardening
**Status**: Resolved (v8.9.99)
**Requirement**: R975

## Description
The system required a strict contract for Tracker and Viewer identities to prevent command injection or role corruption.

## Resolution
- Implemented R975 (Regex: `^[a-zA-Z0-9_-]{1,32}$`).
- Hardened `TrackerService`, `ViewerService`, and `AppNetworkManager` to reject malformed pulses.
- Added `identitySanitizationMigration` in `SettingsRepository` to automatically purge corrupted IDs from storage.
