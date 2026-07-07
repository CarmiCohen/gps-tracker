# Issue #198: GPS Availability Hardening
**Status**: Resolved (v8.9.8)

## Description
GPS stall detection was too slow, leading to long periods of stale data before a revival was attempted.

## Resolution
Shortened GPS stall detection threshold to 60s and revival retry interval to 120s in `TrackerService.kt`.
