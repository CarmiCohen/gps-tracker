# Hardening Phase: Primary Tracking Document (v8.9.52)

This document tracks all open issues, technical debt, and pending validation tasks. Once an item is verified, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Complete | 0 |
| **Validation Tasks** | 🟡 Pending | 1 |
| **Resolved (this session)** | 🟢 Logic & Hardening | 10 |
| **Archived Resolutions** | 📁 Historical | 249 |

---

## ⚠️ Newly Identified Risks & Concerns (v8.9.51)
*   **Watchdog Frequency (Issue #366)**: Frequent `AlarmManager` rescheduling in the service tick loop is throttled by `SYSTEM_WATCHDOG_THROTTLE_MS` (60s), but actual battery impact on varied hardware needs monitoring.
*   **Staleness Jitter (Issue #427/428)**: Aligning staleness thresholds to 10s matching the `PING_INTERVAL_MS` may cause UI flickering on jittery connections. A 15s grace period may be needed for visual stability.

---

## 🔴 Open Technical Issues
*   *(None. All forensic alignment issues resolved in v8.9.52)*

## 🟡 Pending Validation (Hardening Phase)
*   **Issue #452: Forensic SNR Latch Audit**: Final verification of R332/Issue #332 compliance in field tests.

## 🟢 Resolved (this session)
*   **Issue #431: Bayesian Authority Sync**: (Fixed v8.9.52) Implemented uncertainty expansion in `MainAlarmLogic.kt`. Accuracy now grows at 15m/s (Moving) / 1.5m/s (Stationary) capped at 33.3m/s rate during fixes.
*   **Issue #435: Trajectory Forensic Parity**: (Fixed v8.9.52) Hardened GtoEngine and LocationProcessor to preserve maxAccuracy metadata during trajectory promotion, ensuring audit continuity.
*   **Issue #441: Siren Authority Audit**: (Fixed v8.9.52) Refactored `AudioSynthesizer.kt` to use monotonic time for silence latches and aligned auto-stop logs with the 30s mandate.
*   **Issue #440: Log/UI Timing Authority**: (Fixed v8.9.52) Verified 10s startup muzzle in `LogManager.kt` and 50-marker pruning in `MapComponents.kt`.
*   **Issue #433: GtoEngine Optimization Audit**: (Fixed v8.9.52) Verified Towing (10m/s) and Work (5m/s) speed threshold alignment in `GtoEngine.kt`.
*   **Issue #432: Hardware Authority (S21 FE)**: (Fixed v8.9.52) Confirmed 10Hz GPS polling is correctly requested for S21 FE in `ServiceBehaviorUseCase.kt`.
*   **Issue #434: Jump Latch Authority**: (Fixed v8.9.52) Verified 3-minute `JUMP_HOLD_DURATION_MS` is strictly enforced in `MainAlarmLogic.kt`.
*   **Issue #439: Xiaomi Authority Sync**: (Fixed v8.9.52) Synchronized 15s suppression and 60s recovery thresholds in `TrackerService.kt`.
*   **Issue #450: R325 Deduplication Authority**: (Fixed v8.9.52) Hardened `LocationProcessor.kt` to use `maxAccuracy` as the exclusive spatial gate.
*   **Issue #451: Xiaomi Permission Status Reflection**: (Fixed v8.9.52) Verified `UNKNOWN` status handling across `MainAlarmLogic.kt` and `Utils.kt`.

## 🟢 Resolved (previous sessions)
*   **Issue #422: Documentation Desync (Forensic Baseline)**: (Fixed v8.9.51) Updated documentation to reflect Database v50 milestones.
*   **Issue #366: Resilience Hardening - High-Resilience Watchdog**: (Fixed v8.9.51) Implemented a Triple-Lock Watchdog system.
*   **Issue #424: R747 Implementation Paradox**: (Fixed v8.9.51) Standardized Alert Titles and Subtitles.
