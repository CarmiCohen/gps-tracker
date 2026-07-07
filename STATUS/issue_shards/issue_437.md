# Issue #437: Acoustic Floor Calibration Logic
**Status**: Resolved (v8.9.44)
**Requirement**: R810-M

## Description
The acoustic floor calibration logic was inconsistent with the safety gate thresholds.

## Resolution
Aligned `ACOUSTIC_FLOOR_MIN_DB` (50dB) with the authoritative absolute safety gate to prevent false triggers in quiet environments.
