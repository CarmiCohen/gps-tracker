# Issue #014: System-Wide Type Safety
**Status**: Resolved (v9.1.7)
**Priority**: Medium
**Requirement**: R014

## Description
Standardize all telemetry fields (latitude, longitude, speed, accuracy, etc.) to `Double` across the engine and app modules to prevent precision loss and truncation issues.

## Resolution
- Audited all data classes in `:core:engine` and standardized fields to `Double`.
- Updated Room database entities and migrations to support double precision.
- Verified that calculations in `LocationProcessor` and `ImmFilter` use high-precision doubles.
- Added explicit type checks in `handleLocationRelayBinary`.
