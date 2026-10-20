# Issue #1426: HUD Header Visibility & Occlusion

## 🎯 Objective
Fix the UI occlusion in the upper HUD area where the map layer or other elements make the control buttons (Settings, Info, Stats, List, Map) nearly invisible or difficult to interact with.

## 🚩 Problem Statement
In the current Viewer implementation, the upper part of the HUD exhibits poor contrast and visibility. The map background renders behind/through the header buttons, causing them to blend into the terrain, especially in high-detail areas.

## 🛠️ Proposed Mediation
*   **Header Scrim**: Add a solid or semi-opaque background (e.g., `Slate900` at 0.8 alpha) to the top action bar to ensure button legibility.
*   **Z-Index Verification**: Ensure buttons are explicitly drawn on a layer above the map and any telemetry overlays.
*   **Icon Tinting**: Audit icon colors to ensure they meet accessibility standards against the background.

## 📊 Requirements
*   **R-ID 604**: All header navigation buttons MUST be clearly visible and distinguishable from the map background.
