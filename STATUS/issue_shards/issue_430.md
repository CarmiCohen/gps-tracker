# Issue #430: Zeroing Baseline Asymmetry
**Status**: Resolved (v8.9.44)
**Requirement**: R810-P

## Description
Detected asymmetry in the barometric zeroing intervals. 

## Resolution
Aligned `BARO_ZEROING_INTERVAL_MS` (300s) with the `PASSIVE_ZEROING_STATIONARY_MS` baseline to ensure consistent stabilization of altitude metrics when stationary.
