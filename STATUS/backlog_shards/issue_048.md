# Issue #048: Viewer HUD Line Grayout
**Status**: Verified (v9.2.0)
**Priority**: Medium
**Requirement**: R989

## Description
Differentiated "Telemetry Age" (packet) from "GPS Age" (fix) in StatusRowData. 

## Resolution
Connectivity, Battery, and Satellites now remain colorized as long as telemetry is fresh. Distance remains colorized based on last known good position while link is active.
