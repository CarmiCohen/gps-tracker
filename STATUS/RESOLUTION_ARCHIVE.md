# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.02.50 (vSep.02.50)
*   **Issue #005 RESOLVED: Log Spillage Hardening**.
    *   **Problem**: Diagnostic log spillage into `logcat` on Samsung G990 (S21FE) and A15 hardware created forensic risks and could lead to performance degradation during high-frequency telemetry bursts. Direct use of `android.util.Log` bypassed central suppression controls.
    *   **Remediation**: Replaced all direct `android.util.Log` calls with `Timber` in `MaintenanceWorker`, `CommunicationManager`, `MainFileHelper`, `TrackerStateManager`, and `AudioSynthesizer`. Centralized the logging policy in `GpsApplication` to ensure that only critical errors (sanitized at the edge) are emitted in release builds. Established Architectural Rule 1.18 (R759) to strictly prohibit direct `Log` calls, ensuring long-term silence on audited hardware (R-ID 239).

## 🟢 Sep.03.01 (vSep.03.01)
*   **Issue #197 RESOLVED: Forensic Teardown Timing Logs**.
    *   **Problem**: Identifying OS-level disposal delays during component unregistration required high-precision timing across all core services to match the hardware layer's auditing.
    *   **Remediation**: Enhanced `ConnectivitySuite.stop()` and `CommunicationManager.disconnect()` with forensic duration tracking. Implemented high-precision log summaries to report unregistration times for network callbacks and socket cleanup, ensuring forensic parity across the entire teardown sequence (R-ID 197).
*   **Issue #238 RESOLVED: Location Model Unification**.
    *   **Problem**: Redundant mapping between `LocationUpdate` (Engine) and `LocationState` (UI) caused excessive allocation churn on high-frequency telemetry updates.
    *   **Remediation**: Promoted the core engine `LocationUpdate` model to be the unified source of truth. Removed the obsolete `LocationState` class and refactored `MainUiState`, `KinematicState`, and `TelemetryUseCase` to consume the unified model directly. This eliminates O(N) mapping overhead and reduces GC pressure during active tracking sessions (R-ID 238).

## 🟢 Sep.02.46 (vSep.02.46)
*   **Issue #118 RESOLVED: 16KB Page Size Compatibility**.
    *   **Problem**: Android 15 requires native libraries to be aligned to 16KB boundaries to support devices with larger page sizes.
    *   **Remediation**: Updated `app/src/cpp/CMakeLists.txt` with `-Wl,-z,max-page-size=16384` linker flag. Set `android:extractNativeLibs="false"` in `AndroidManifest.xml` (R895).
*   **Issue #120b RESOLVED: SIT Forensic Timestamp Validation**.
    *   **Problem**: High-precision forensic analysis required wall-clock and monotonic timestamps for the exact moment of peak vertical velocity during a SIT event.
    *   **Remediation**: Integrated `sitVzTs` and `sitVzRt` into core engine, aggregator, and persistence layers. Verified Room migrations 71-73 (R172).

---
*For older resolutions, see prior sub-versions.*
