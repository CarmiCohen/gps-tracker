# Issue #039: Identity Rejection Feedback
**Status**: Resolved (v9.3.4)
**Priority**: High
**Requirement**: R977

## Description
The system must provide explicit UI feedback when a user attempts to save or commit an identity (Tracker ID or Viewer ID) that results in a collision or validation failure.

## Resolution
- Updated `MainRepository.saveSettingsBulk` to throw `IllegalArgumentException` on identity collision.
- Implemented error handling in `MainViewModel` to catch rejection events.
- Added UI feedback via Toasts and persistent pink forensic logs when a collision occurs.
- Validation tracked in #063.
