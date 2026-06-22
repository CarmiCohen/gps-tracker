# Project Issues: Contradictions, Inconsistencies & Hardening

This file tracks the status of identified contradictions between the System Source of Truth (SoT) and the implementation, as well as pending technical hardening tasks.

## 1. Constant Name Mismatch (Issue #228)
*   **Description**: `ACK_SYNC_LOOP_INTERVAL_MS` was defined in the SoT, but the implementation used `PING_INTERVAL_MS`.
*   **Status**: **Resolved**. SoT updated to use `PING_INTERVAL_MS` (10,000ms) for both relay heartbeats and log synchronization.

## 2. System Versioning Inconsistency (Issue #203)
*   **Description**: Baseline mismatch between documentation (v8.9.26) and core service headers (v8.9.24).
*   **Status**: **Resolved**. All code headers and SoT synchronized to **v8.9.27**.

## 3. Latitude Conversion Precision (Issue #228)
*   **Description**: Precision drift in `LAT_DEG_TO_METERS` physical constant between documentation (4 decimals) and code (11 decimals).
*   **Status**: **Resolved**. Both Code and SoT now use the high-precision constant: `111194.92664455874`.

## 4. Version Generation Logic (Issue #199)
*   **Description**: Mismatch in `versionCode` logic (Git count vs Timestamp).
*   **Status**: **Resolved**. SoT updated to reflect Git-based versioning; implementation validated as superior for build reproducibility.

## 5. Standardized Alert Title Inconsistency (Issue #230)
*   **Description**: Storage and Xiaomi alerts used the "This device:" prefix in code but the "Tracker:" prefix in the SoT.
*   **Status**: **Resolved**. Code updated to use **"Tracker:"** prefix as per SoT Manifest for consistent remote attribution.

## 6. Documentation Internal Contradiction (Issue #211)
*   **Description**: `DEVICE_SPECIFIC_ADAPTATIONS.md` states 1000ms gap threshold, but SoT/Code define 200ms.
*   **Location**: `DEVICE_SPECIFIC_ADAPTATIONS.md` (Section 2.6).
*   **Status**: **Open**. Requires update to adaptation documentation.

## 7. Role Prefix Enforcement (Issue #182)
*   **Description**: Enforced "T"/"C" ID prefixes are undocumented in the forensic identity section of the SoT.
*   **Status**: **Resolved**. Section 4.1 of SoT updated with role identity standards.

## 8. Jump Classification Conflict (Issue #231)
*   **Description**: Engine logic classifies extreme jumps as `OUTLIER`, but the SoT manifest implied they were grouped under `JUMP_ALERT`.
*   **Status**: **Resolved**. SoT Manifest (Section 8) clarified to show Outliers are filtered under the `JUMP_ALERT` security tier.

## 9. Foreground Resilience Hardening (Issue #218)
*   **Description**: `TrackerService.kt` recovery pulses lack `try-catch` protection against Android 14+ `ForegroundServiceStartNotAllowedException`.
*   **Location**: `TrackerService.kt`.
*   **Status**: **Open**.

## 10. Xiaomi Boot Verification (Issue #190)
*   **Description**: Xiaomi Boot Grace logic (`XIAOMI_BOOT_GRACE_MS`) requires physical hardware verification to ensure zero-spike behavior during transition.
*   **Location**: `MainAlarmLogic.kt` / Physical Hardware.
*   **Status**: **Open**.

## 11. Hindsight Promotion Coverage (Issue #227)
*   **Description**: `LocationSentinel`'s new `promotedPoints` logic for hindsight smoothing lacks exhaustive unit test coverage for multi-point transitions.
*   **Location**: `:core:engine` Tests.
*   **Status**: **Open**.

## 12. SIT Duplicate Guard (Issue #245)
*   **Description**: Redundant SIT log triggers removed from RemoteHandler, but HistoryManager still lacks a database-level sanity check to prevent duplicates if the relay sends redundant packets.
*   **Location**: `HistoryManager.kt`.
*   **Status**: **Open**.

## 13. Hardcoded EMA in AppSensorManager (Issue #234)
*   **Description**: `AppSensorManager.kt` uses a hardcoded `0.01f` for light baseline EMA instead of the `LUX_EMA_FAST` constant.
*   **Location**: `AppSensorManager.kt`.
*   **Status**: **Open**.

## 14. Light EMA Logic Inconsistency (Issue #234)
*   **Description**: SoT defines rising/falling EMA factors for Light (UP_SLOW, UP_FAST, etc.), but the engine (`LocationSentinel.kt`) only implements a single `LUX_EMA_FAST` factor.
*   **Location**: `LocationSentinel.kt`.
*   **Status**: **Open**.
