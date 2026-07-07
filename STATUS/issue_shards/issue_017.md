# Issue #017: SnapshotStateList Lock Failures
**Status**: Resolved
**Priority**: Medium

## Description
Replaced observable pools in map updates with `SnapshotStateList` to prevent lock failures and improve UI thread stability. (v8.9.81)
