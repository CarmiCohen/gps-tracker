# Forensic Handover - v9.3.0 (Map Metadata Realignment)

## 📌 Status: Stable / Build PASS / Release Ready
This cycle implements R400, optimizing the spatial UX by moving Bayesian uncertainty messages away from the map center.

### 🟢 Completed: Requirement R400 (Map Metadata Alignment)
*   **UX Realignment**: Moved "UNCERTAINTY: ..." status messages from the center of the `AppMapContainer` to the bottom-center.
*   **Collision Prevention**: Implemented an 80dp vertical padding to ensure clear separation from the `osmdroid` scale bar, which is centered at the bottom.
*   **Visual Integrity**: Preserved the `Amber500` background and high-contrast text to maintain visibility against varying map tiles.

### 🟢 Pre-existing State: v9.2.9
*   **R994 (Screen-Off Optimization)**: 5s polling throttle when device is locked.

### 🛠 Instructions for Resumption
1.  **Verification of R400**:
    *   Trigger a "SIGNAL LOSS" or "GPS GAP" state.
    *   Verify the uncertainty message appears at the bottom-center of the map.
    *   Confirm it does not obscure the tracker icon or the map scale bar.

---
*Generated for chat resumption. All authoritative documents (SoT, issues, archive) are synchronized.*
