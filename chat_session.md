# Project Revolution: Forensic Resumption Blueprint (v246 - Revol30.0)
**Date**: October 30, 2024
**Status**: Revol30.0 Progress. A15 Paired. Sensor App Hardening in Progress.
**Resumption Token**: REVOLUTION-SENSOR-HARDENING-A15-V1

## 1. Executive Summary: Sensor Hardening & API 34 Compatibility
The `:app-monitor` is successfully paired on the A15 device. However, `:app-sensor` is encountering persistent `SecurityException`s on start, despite permissions being declared in the manifest. This suggests a synchronization issue between the build and the device's permission registry, or a race condition in the service startup.

*   **Tracker ID**: `3912e6f27b42a6d41c7c6e0c99526665377ebf2f6a434d83203b81a5e8e54b44`.
*   **Target Device**: A15 (R58X40GV2AR).
*   **Current Issue**: `SecurityException` for `HIGH_SAMPLING_RATE_SENSORS` and `ACCESS_NETWORK_STATE` despite manifest declarations.
*   **Current Action**: Synchronizing Gradle and performing a full build to ensure manifest integrity.

## 2. Technical Remediation Strategy
- **Manifest Registry**: Force a full redeploy of `:app-sensor` after a Gradle sync to ensure the A15 device acknowledges the new permission requests.
- **Service Delay**: Implemented a fallback to `SENSOR_DELAY_NORMAL` to avoid sampling rate restrictions on API 34+.
- **Permission Guard**: Hardened UI to prevent service start until location and notification permissions are confirmed.

## 3. Current Build Status & Blockers
- **Status**: Code updated. Deployment sync pending.
- **Blocker**: Device-side "App keeps stopping" dialog from previous failed runs.

## 4. Planned Remediation Sequence
1.  **Gradle Sync**: `gradle_sync()` to align project state.
2.  **Assemble**: `gradle_build(":app-sensor:assembleDebug")`.
3.  **Redeploy**: `deploy()` `:app-sensor`.
4.  **UI Verification**: Launch `AdminActivity`, grant permissions via `PermissionGuard`.
5.  **Service Start**: Tap "START SENSOR SERVICE" and verify lifecycle in Logcat.

## 5. Traceability Record
- **SOT Count**: 874 Rules.
- **Audit State**: ACTIVE (Revol30.0).

**BLUEPRINT UPDATED. SYNCHRONIZING GRADLE.**
