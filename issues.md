# Project Issues & Hardening Tracking (July.19.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 295 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Samsung-specific ANR on Migration**: Even with IO offloading, extremely large log tables (1000+ entries) might cause slow startup during the table recreation on lower-end devices like the A15.
*   **Anchor Sensitivity**: The new `ANCHOR_DISPLACEMENT_WEIGHT` for Issue #062 might require hardware-specific tuning if urban canyon jitter triggers false breakouts on older devices.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (July.19.02)
*   **Issue #100: Relay Wake-up Timeout Hardening**.
    *   **Root Cause**: Insufficient 30s timeout for relay infrastructure cold-starts.
    *   **Resolution**: Increased `NETWORK_TIMEOUT_MS` to 60s in `EngineConstants.kt` and synchronized `AppNetworkManager.kt` to use this centralized constant for both keep-alive and wake-up attempts.
    *   **Requirement Alignment**: Satisfies R404 (Relay Resilience).

## 🟢 Recently Resolved Issues (July.19.01)
*   **Issue #099: Main Thread Frame Skipping (Cold Start)**.
    *   **Root Cause**: Main thread starvation during cold start due to simultaneous composition, IPC permission checks, and reactive flow initialization.
    *   **Resolution**: Implemented `INITIAL_RENDER_DELAY_MS` (500ms) staggering in `MainViewModel`. Offloaded and cached hardware property checks (e.g., `isA15Device`) in `SystemStatusProvider` to eliminate redundant IPC calls during the critical render window.
    *   **Verification**: Verified via logcat that frame skipping warnings are eliminated on low-end hardware during Landing Page entry.

*   **Issue #098: Hardware Step Detector Registration Failure (Samsung A15)**.
    *   **Root Cause**: `sensorManager.registerListener` returning `false` for Step Detector despite a non-null sensor object.
    *   **Resolution**: Added `isStepDetectorRegistered` flag in `AppSensorManager`. The Accelerometer stay-alive fallback (R405) now triggers if registration fails.
    *   **Verification**: Verified via logging that Accelerometer pulses commence if Step Detector registration fails.

*   **Issue #101: Silent Battery Exemption Requirement (Samsung A15)**.
    *   **Root Cause**: Lack of UI trigger in `MainActivity` despite background detection on Samsung A15.
    *   **Resolution**: Explicitly triggered `PhoneSetupOverlay` in `onResume` when battery whitelist is missing on A15 hardware.
    *   **Verification**: HUD/Setup UI now appears automatically upon app launch if requirements aren't met.
