# Issue #436: Stationary GPS Pulse Asymmetry
**Status**: Resolved (v8.9.43)
**Requirement**: R810-P

## Description
Detected asymmetry in GPS pulse timing when stationary.

## Resolution
Aligned `GPS_SAVE_INTERVAL_MS` (20s) with `STATIONARY_GPS_POLLING_MS` to ensure consistent data recording during long idle periods.
