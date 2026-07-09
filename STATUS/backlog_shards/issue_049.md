# Issue #049: HUD Context Mapping Authority
**Status**: Resolved (v9.2.6)
**Priority**: High
**Requirement**: R049

## Description
HUD shows “P” adjacent to name and “JAMMER…” label incorrectly on Tracker line. This indicated a logic error in the UI mapping for the local tracker status.

## Resolution
- Corrected `GlobalStatusBar` mapping to use mode-aware location context.
- Ensured that local hardware status (Jammer/Privacy) is only applied to the authoritative "This Device" line.
- Implemented mode-aware telemetry binding to prevent cross-contamination of peer status onto local badges.
