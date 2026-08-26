# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Aug.26.11 (vAug.26.11)
*   **Issue #735 Hardening**: **Setup Overlay Bypass**. Implemented a developer-mode bypass for the `PhoneSetupOverlay` to allow automated soak tests to proceed without manual permission granting.
    *   Added `isSetupBypassActive` to `MainUiState` to override `isSystemReady` safety checks.
    *   Integrated a "Setup Overlay Bypass" toggle in the `DiagnosticsScreen` under Validation Hooks.
    *   Updated `PhoneSetupOverlay` to display a "BYPASS ACTIVE" banner and provide a "DISMISS" button when the bypass is enabled, even if system permissions are missing.

## 🟢 Aug.26.10 (vAug.26.10)
*   **Deployment Verification**: Formally verified **Issue #723 (StackLog Leak)** and **Issue #320 (Hardware Handshake)** on SM-A155F hardware. 
    *   Logcat confirmed the `StackLog` trace appears only once during initial registration, validating the `SharingStarted.Eagerly` fix.
    *   Hardware handshake logic verified as stable during service lifecycle transitions.
*   **New Concern Captured**: Identified **Issue #735: Setup Overlay Modal Block**, where the mandatory setup screen prevents service start until manual battery/overlay permissions are granted.

## 🟢 Aug.26.09 (vAug.26.09)
*   **Issue #320**: **Hardware Handshake**. Replaced the 200ms "magic" settling delay in `TrackerService.onDestroy()` with a deterministic native round-trip (`punchHardware`). This ensures the native event queue is drained and the JNI bridge is responsive before release, preventing race conditions during service destruction on budget hardware (A15).

---
*For historical entries, see legacy logs.*
