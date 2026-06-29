# Hardening Phase: Primary Tracking Document (v8.9.51)

This document tracks all open issues, technical debt, and pending validation tasks. Once an item is verified, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 7 |
| **Validation Tasks** | 🟡 Pending | 3 |
| **Resolved (this session)** | 🟢 Documentation | 27 |
| **Archived Resolutions** | 📁 Historical | 231 |

---

## ⚠️ Newly Identified Risks & Concerns (v8.9.51)
*   **Watchdog Frequency (Issue #366)**: Frequent `AlarmManager` rescheduling in the service tick loop is throttled by `SYSTEM_WATCHDOG_THROTTLE_MS` (60s), but actual battery impact on varied hardware needs monitoring.
*   **Staleness Jitter (Issue #427/428)**: Aligning staleness thresholds to 10s matching the `PING_INTERVAL_MS` may cause UI flickering on jittery connections. A 15s grace period may be needed for visual stability.

---

## 🔴 Open Issues (Critical Contradictions & Inconsistencies)

## 🔴 Open Technical Issues
*   **Issue #431: Missing Bayesian Authority**: SoT omits the 33.3m/s safety cap for uncertainty expansion.
*   **Issue #432: Missing Hardware Authority (S21 FE)**: SoT omits background activity requirement for 10Hz GPS.
*   **Issue #433: Missing GtoEngine Optimization Coverage**: GTO speed thresholds (Tow/Work) absent from SoT.
*   **Issue #434: Jump Latch Authority Omission**: 3-minute `JUMP_HOLD_DURATION_MS` omitted from SoT.
*   **Issue #439: Missing Xiaomi Authority**: SoT omits 15s suppression and 60s recovery thresholds.
*   **Issue #440: Missing Log/UI Timing Authority**: Startup muzzles (10s) and marker pruning thresholds missing from SoT.
*   **Issue #441: Missing Siren Authority**: 30s auto-stop (Issue #429) and 15s resume cooldown missing from SoT.

## 🟡 Pending Validation (Hardening Phase)
*   **Issue #450: R325 Deduplication Authority**: Verify `maxAccuracy` usage in `LocationProcessor.kt` for point deduplication.
*   **Issue #451: Xiaomi Permission Status Reflection**: Verify `UNKNOWN` status handling on legacy MIUI.
*   **Issue #452: Forensic SNR Latch Audit**: Verify R332/Issue #332 compliance in Jump Engine.

## 🟢 Resolved (this session)
*   **Issue #422: Documentation Desync (Forensic Baseline)**: (Fixed v8.9.51) Updated `DATA_STORAGE_AND_FILES.md` and `LOCATION_SENTINEL_SPEC.md` to reflect Database v50 milestones, including Dual-Metric Spatial Anchors and Hindsight Correction.
*   **Issue #366: Resilience Hardening - High-Resilience Watchdog**: (Fixed v8.9.51) Implemented a Triple-Lock Watchdog system. Layer 3 (WorkManager) scheduled in `GpsApplication.kt`. Layer 2 (AlarmManager) chain hardened in `BaseMonitorService.kt`.
*   **Issue #424: R747 Implementation Paradox**: (Fixed v8.9.51) Standardized Alert Titles and Subtitles per R747 mapping. Removed redundant "Tracker:" prefixes and localized Viewer hardware events to "This device".
*   **Issue #429: Acoustic Hysteresis Paradox**: (Fixed v8.9.50) Aligned `SIREN_AUTO_STOP_MS` (30s) with `ACOUSTIC_RECOVERY_DELAY_MS` in `EngineConstants.kt`. Resolved the discrepancy where UI feedback lingered 15s past engine recovery.
*   **Issue #365: Ghost Mode Status Conflict**: (Fixed v8.9.50) Unified `isDataHealthy` logic and propagated local telemetry freshness to hardware sensor rows in `SharedUiComponents.kt`. Resolved the Paradox where local staleness was suppressed.
*   **Issue #427: UX Inconsistency - Status Badge Staleness**: (Fixed v8.9.49) Aligned GPS and TRK badge thresholds to the authoritative 10s R338 mandate. Synchronized `WATCH_TIMEOUT_MS` and `WATCH_DOG_UI_GRACE_MS` in `EngineConstants.kt`.
*   **Issue #428: R338 Compliance Gap (Data Healthy Badge)**: (Fixed v8.9.49) Synchronized the DAT badge with the 10s telemetry staleness gate, decoupling it from the 30s heartbeat.
*   **Issue #438: Issue ID Mismatch (Power Forensics)**: (Fixed v8.9.48) Unified all Power Forensic references (`currentMa`) to the authoritative **Issue #337** across source code.
*   **Issue #425: R865 Color Non-Compliance (UI Identity)**: (Fixed v8.9.48) Aligned all status badges and indicators with the authoritative `BrandJd` (#367C2B) color mandate.
*   **Issue #421: Role Identity Prefix Mismatch (R182)**: (Fixed v8.9.46) Aligned UI logic and string resources with the authoritative "T" and "C" prefixes mandated by SoT R182.
*   **Issue #430: Zeroing Baseline Asymmetry**: (Fixed v8.9.44) Aligned `BARO_ZEROING_INTERVAL_MS` (300s) with the `PASSIVE_ZEROING_STATIONARY_MS` baseline.
*   **Issue #437: Acoustic Floor Calibration Logic**: (Fixed v8.9.44) Aligned `ACOUSTIC_FLOOR_MIN_DB` (50dB) with the authoritative absolute safety gate.
*   **Issue #435: Hindsight Buffer Desync**: (Fixed v8.9.43) Expanded `HINDSIGHT_BUFFER_SIZE` to 10 points to align with Forensic Spec.
*   **Issue #436: Stationary GPS Pulse Asymmetry**: (Fixed v8.9.43) Aligned `GPS_SAVE_INTERVAL_MS` (20s) with `STATIONARY_GPS_POLLING_MS`.
*   **Issue #426: Logic Regression - GPS Staleness Coupling**: (Fixed v8.9.43) Decoupled GPS health from heartbeat status.
*   **Issue #423: R325 Authority Scope Discrepancy**: (Fixed v8.9.43) Hardened `LocationProcessor.kt` to use `maxAccuracy` as the authoritative gate for trajectory deduplication.
*   **Issue #364: Logic Error - GPS Freshness**: (Fixed v8.9.42) Decoupled GPS health from the general heartbeat.
*   **Issue #360: Logic Alignment - Jump Threshold**: (Fixed v8.9.42) Replaced hardcoded 5.0m/s.
*   **Issue #361: Documentation Refactor**: (Fixed v8.9.42) Bulk migrated legacy ID references.
*   **Issue #362: Regression Fix - Xiaomi Key**: (Fixed v8.9.42) Corrected `IS_XIAOMI_MANUAL_OVERRIDE_KEY`.
*   **Audit Resolution (SoT Sync)**: Authoritatively documented Battery (20%), Signal Loss (180s/30s), Bayesian Growth (15m/s), GtoEngine Thresholds, and SIT Timing Gates in `REQUIREMENTS_SoT.md`.
