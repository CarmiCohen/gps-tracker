# Issue #278: TIMING MISMATCH (GPS Gap)
**Status**: Resolved (Historical)

## Description
Migrated `lastValidFixTs` to monotonic time in `LocationProcessor.kt` to resolve timing mismatches during GPS signal gap detection. (v8.8.12 / Formerly #76)
