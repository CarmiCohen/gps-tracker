# Issue #030: Proto Schema Duplication
**Status**: Resolved (v9.3.0)
**Priority**: Medium
**Requirement**: R973

## Description
Consolidated all protobuf schemas into `app/src/main/proto` and removed the legacy path `app/src/proto` to prevent synchronization drift and build inconsistencies.

## Resolution
- Verified all `.proto` files are present in the authoritative path.
- Updated `build.gradle` to reference the single source directory.
- Deleted the legacy `app/src/proto` directory.
- Verified that both `:app` and `:core:engine` generate code from the same source.
