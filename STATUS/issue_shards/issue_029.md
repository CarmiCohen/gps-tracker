# Issue #029: Viewer Status Line Grayed-Out
**Status**: Resolved (v9.0.3)
**Requirement**: R989

## Description
Updated `ViewerService.kt` to propagate local `LocationUpdate` telemetry to the repository. This ensures the Viewer's local metadata (battery, thermal, position) is processed by the UI's staleness logic, keeping the row active and colored in authoritative role colors.
