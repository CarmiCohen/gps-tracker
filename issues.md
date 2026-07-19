# Project Issues & Hardening Tracking (July.19.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 293 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Samsung-specific ANR on Migration**: Even with IO offloading, extremely large log tables (1000+ entries) might cause slow startup during the table recreation on lower-end devices like the A15.
*   **Anchor Sensitivity**: The new `ANCHOR_DISPLACEMENT_WEIGHT` for Issue #062 might require hardware-specific tuning if urban canyon jitter triggers false breakouts on older devices.

---

## 🔴 Open Issues

*   **Issue #099: Main Thread Frame Skipping (Cold Start)**.
    *   **Symptoms**: `Skipped 53 frames!` and `Skipped 40 frames!` warnings during Landing Page initialization.
    *   **Impact**: Risks ANR on low-end hardware; violates the spirit of R952 (Reactive Setup Flow).
    *   **Task**: Audit `MainActivity.onCreate` and `MainViewModel` init for remaining blocking calls (e.g., Battery optimization checks).

*   **Issue #100: Intermittent Relay Wake-up Timeouts**.
    *   **Symptoms**: `Connection timed out` observed during `Wake-up (Attempt 1)` in `AppNetworkManager`.
    *   **Impact**: Delayed tracking startup and potential loss of initial telemetry if the relay is slow to spin up.
    *   **Task**: Increase initial wake-up timeout in `AppNetworkManager` and implement a more aggressive retry strategy for the first connection.

---

## 🟢 Recently Resolved Issues (July.19.00)
*   **Issue #098: Hardware Step Detector Registration Failure (Samsung A15)**.
    *   **Root Cause**: `sensorManager.registerListener` returning `false` for Step Detector despite a non-null sensor object, bypassing previous fallback logic.
    *   **Resolution**: Added `isStepDetectorRegistered` flag in `AppSensorManager` to track actual registration success. The Accelerometer stay-alive fallback (R405) now triggers if registration fails.
    *   **Verification**: Verified via logging that Accelerometer pulses commence if Step Detector registration fails.

*   **Issue #101: Silent Battery Exemption Requirement (Samsung A15)**.
    *   **Root Cause**: Lack of UI trigger in `MainActivity` despite background detection on Samsung A15.
    *   **Resolution**: Explicitly triggered `PhoneSetupOverlay` in `onResume` when battery whitelist is missing on A15 hardware.
    *   **Verification**: HUD/Setup UI now appears automatically upon app launch if requirements aren't met.

*   **Issue #097: Room Database Identity Hash Mismatch (IllegalStateException)**.
    *   **Root Cause**: Discrepancy between manual SQL in `MIGRATION_55_56` and Room's expected schema (identity hash) for version 56.
    *   **Resolution**: Bumped version to 57. Added `MIGRATION_56_57` which performs a robust "create-new-copy-old-rename" sequence for all tables to strictly align with Entity definitions.
    *   **Verification**: App initializes database successfully and resumes tracking without integrity errors.
