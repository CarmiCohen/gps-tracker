# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.02.40 (vSep.02.40)
*   **Issue #896 RESOLVED: Battery Optimization Navigation Failure**.
    *   **Problem**: The "Open Settings" button in `PhoneSetupOverlay` for Battery Mode failed to trigger the `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent on Samsung A15 devices, leaving the user unable to easily access the critical "Unrestricted" battery setting.
    *   **Remediation**: Implemented robust battery navigation (R896). Hardened `MainActivity.launchBatteryExemptionSetting` with a multi-tier fallback: (1) Direct request via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, (2) General optimization list via `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, and (3) Full App Info details via `ACTION_APPLICATION_DETAILS_SETTINGS`. Standardized all permission-related intents to use `Uri.fromParts("package", pkg, null)` to ensure guaranteed URI encoding compatibility on Android 15+.

## 🟢 Sep.02.27 (vSep.02.27)
*   **Issue #895 RESOLVED: Android 15 16KB Page Size Compatibility**.
    *   **Problem**: Native libraries were not aligned to 16KB boundaries, risking load failures on future Android 15 production builds.
    *   **Remediation**: Aligned native libraries to 16KB (R895). Upgraded AGP to 8.5.1, set `useLegacyPackaging = false`, and added `-Wl,-z,max-page-size=16384` linker flags.

## 🟢 Sep.01.27 (vSep.01.27)
*   **Issue #893 RESOLVED: Native Resource Disposal Leak (BaseEventQueue)**.
    *   **Problem**: Looper mismatch during `ConnectivityManager` unregistration caused native disposal warnings.
    *   **Remediation**: Standardized looper alignment (R893) by ensuring all network callbacks register/unregister on the `MainLooper`.

---
*For older resolutions, see prior sub-versions.*
