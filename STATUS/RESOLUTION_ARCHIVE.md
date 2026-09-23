# 🏛️ Resolution Archive - Sep.23.04

## 🏁 Issue #1200: Shared Overlay Scope
*   **Resolved**: Sep.23.04
*   **Root Cause**: Feature screens (`TrackerScreen`, `ViewerScreen`) were manually rendering and managing callbacks for shared overlays (Settings, Log, Ribbons, GNSS Detail), leading to significant code duplication and complex callback propogation.
*   **Remediation**:
    *   Implemented a centralized `OverlayHost` component in `MainAppContent`.
    *   Exposed global telemetry and log flows in `MainViewModel`.
    *   Removed local overlay rendering logic from feature screens.
    *   Wired all overlay actions directly to `MainViewModel` event handlers.
*   **R-ID**: 420

## 🏁 Issue #1192: Disconnected Settings Input State Flow
*   **Resolved**: Sep.23.03
*   **Root Cause**: User input for device configuration was being trapped in feature-specific ViewModels while the UI observed the global `MainViewModel` for state, causing an input lock.
*   **Remediation**:
    *   Centralized all configuration draft and commit logic into `MainViewModel`.
    *   Refactored `TrackerScreen` and `ViewerScreen` to route settings events to `MainViewModel`.
    *   Removed redundant draft handling code from `TrackerViewModel` and `ViewerViewModel`.
    *   Integrated settings commitment into the navigation back-handler in `MainAppContent`.
*   **R-ID**: 419

## 🏁 Issue #1193: Asymmetric Audio Control and Siren State Dispersion
*   **Resolved**: Sep.23.01
*   **Root Cause**: Siren playback feedback was disconnected from the actual synthesis engine, relying on dispersed state in `diagnosticState` that wasn't reactively updated by `AudioSynthesizer`.
*   **Remediation**:
    *   Converted `AudioSynthesizer.isLooping` to a `MutableStateFlow` (`isSirenPlaying`).
    *   Updated `MainViewModel` to observe this flow and sync it with `DiagnosticState`.
    *   Fixed a critical typo in `app_settings.proto` that broke the build pipeline.
*   **R-ID**: 418
