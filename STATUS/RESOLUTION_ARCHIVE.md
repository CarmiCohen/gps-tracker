# Resolution Archive

This document archives all resolved issues and architectural refinements.

## 🟢 Sep.02.46 (vSep.02.46)
*   **Issue #118 RESOLVED: 16KB Page Size Compatibility**.
    *   **Problem**: Android 15 requires native libraries to be aligned to 16KB boundaries to support devices with larger page sizes.
    *   **Remediation**: Updated `app/src/main/cpp/CMakeLists.txt` with `-Wl,-z,max-page-size=16384` linker flag. Set `android:extractNativeLibs="false"` in `AndroidManifest.xml` to ensure the OS can map the library directly from the APK with proper alignment. Verified compliance across the `jdHardware` module (R895).
*   **Issue #120b RESOLVED: SIT Forensic Timestamp Validation**.
    *   **Problem**: High-precision forensic analysis required wall-clock and monotonic timestamps for the exact moment of peak vertical velocity during a SIT event to ensure telemetry parity.
    *   **Remediation**: Integrated `sitVzTs` and `sitVzRt` into the core engine (`LocationSentinel`), forensic aggregator (`TelemetryAggregator`), and persistence layer (`HistoryEntity`, `PendingStatusEntity`). Verified that Room migrations (71-73) correctly provisioned these fields and that they are correctly mapped during tracker-to-viewer relay (R172).

## 🟢 Sep.02.45 (vSep.02.45)
*   **Issue #122 RESOLVED: Hardware Settling Window Verification**.
    *   **Problem**: Potential native race conditions during hardware teardown on Android 15/Samsung A15 hardware required verification that the 800ms settling window was effective and that all listeners were unregistered before thread death (R891).
    *   **Remediation**: Enhanced `HardwareProvider.stop()` with an `isTeardownActive` atomic gate to prevent callbacks during disposal. Implemented forensic duration tracking for each component (GNSS, Location, Sensors, Display) and added a teardown summary report. Verified that the 800ms settling window provides sufficient buffer for OS-level cleanup.

---
*For older resolutions, see prior sub-versions.*
