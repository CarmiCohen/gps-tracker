# Issue #1425: Version String Alignment Fix

## 🎯 Objective
Relocate the version identifier to the absolute bottom of the Landing/Viewer screens to ensure visual consistency and compliance with branding guidelines.

## 🚩 Problem Statement
The version string (e.g., `Oct20.31`) is currently floating above the bottom edge of the screen, leaving an awkward gap. It should be pinned to the bottom-most part of the UI.

## 🛠️ Proposed Mediation
*   **Layout Constraint Update**: Adjust the vertical constraints or padding in the footer section to align the version text box with the screen bottom.
*   **Safe Area Audit**: Ensure that while it is at the bottom, it doesn't conflict with system navigation bars (gesture bar).

## 📊 Requirements
*   **R-ID 603**: The version string box MUST be positioned at the bottom-most available coordinate of the UI container.
