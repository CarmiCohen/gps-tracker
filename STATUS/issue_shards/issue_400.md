# Issue #400: Map Metadata Alignment (R400)
**Status**: Resolved (v9.3.0)
**Priority**: Medium

## Description
Map-level status messages (e.g., "UNCERTAINTY: ...") were previously anchored to the center of the screen, potentially obscuring the tracker icon or focal points. 

## Resolution
Re-anchored messages to the bottom-center of the `AppMapContainer`. Implemented an 80dp vertical offset to ensure no overlap with the `osmdroid` scale bar.

## Mapping Note
This issue formally adopts the **R400** requirement. It was previously misidentified as **#496** in legacy tracking; **#496** has been retired for this context and redirected to **#326**.
