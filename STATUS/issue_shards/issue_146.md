# Issue #146: Startup Performance (Skipped Frames)
**Status**: Resolved (Historical)

## Description
Moved `OsmConfig` and other heavy initialization tasks to a background thread to prevent skipping frames during application startup. (8.8.35)
