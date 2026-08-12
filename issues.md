# Project Issues & Hardening Tracking (Aug.11.20)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 AT RISK | 5 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 585 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #151] Phone Setup ANR**: A 5000ms+ main-thread stall (ANR) occurred on Samsung A15 hardware while navigating to the Phone Setup overlay. This suggests that "Staggered Incremental Hydration" (R142) is insufficient or being bypassed by a blocking I/O operation during the transition.
*   **[Issue #146] Drain Convergence**: The forensic drainer in `LogRepository` is showing significant latency spikes (up to 198ms) during "High Pressure" buffer states. Confirmed via log monitoring. Hot-path requires optimization.
*   **[Issue #147] Version Inconsistency**: Critical documentation mismatch. `Handover.md` and `issues.md` track `Aug.11.20`, but `build.gradle` and UI remain at `Aug.11.08`.
*   **[Issue #148] Header Layout Inversion**: The `HeaderBar` composable's visual output is reversed compared to the `Row` declaration order in `SharedUiComponents.kt`. This suggests an unintended RTL override or arrangement logic error.
*   **[Issue #150] R405 Detection Bypass**: Automated Phone Setup prompt (R405) failed to trigger on verified Samsung A15 (SM-A155F) hardware despite missing battery exemptions.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.11.20)
*   **[Issue #145] [Severity: High] [Category: Logic] Forensic Spill-Buffer Overflow Protection.**
    *   **Resolution**: Hardened the **Forensic Sampling Authority (R669/R700)**. Implemented proactive pressure-aware throttling in the `TrackerService`. The system now monitors the `MappedByteBuffer` fill level; when it exceeds 80% (`HIGH_PRESSURE_THRESHOLD`), the forensic sampling interval is automatically increased to `FORENSIC_SAMPLING_INTERVAL_THROTTLED_MS` (250ms). (R669)

---

## 🟢 Recently Resolved Issues (Aug.11.16)
*   **[Issue #144] [Severity: High] [Category: Logic] Geofence Uncertainty Growth Validation.**
    *   **Resolution**: Hardened the **Bayesian Uncertainty Authority (R460)** by fixing a flaw in the geofence hysteresis logic. The system now uses the time-drifted uncertainty (`acc`) instead of the static accuracy of the last valid fix when determining if a device has "returned to safe range". (R460)

---

## 🟢 Recently Resolved Issues (Aug.11.13)
*   **[Issue #141] [Severity: Low] [Category: Performance] Stress Recovery Verification.**
    *   **Resolution**: Hardened the system's return-to-baseline logic post-saturation. Implemented `resetSimulatedAnomalies()` in `SystemMonitor`. Integrated dynamic hardware GPS polling (R406a) and implemented a 5000ms "Adaptation Muzzle" (ADAPTATION_SETTLING_MS). (R141)

---

## 🟢 Recently Resolved Issues (Aug.11.08)
*   **[Issue #143] [Severity: High] [Category: Forensic] Forensic Integrity Verification.**
    *   **Resolution**: Hardened the **Silent Failure Correlation Engine (R133)** by linking thermal safety states (Cooling Mode) to location stall detection. Expanded `SystemHealthState` to include `isThermalThrottling`. (R143)

---

## 🟢 Recently Resolved Issues (Aug.11.07)
*   **[Issue #142] [Severity: High] [Category: Performance] Phone Setup Overlay Stabilization.**
    *   **Resolution**: Implemented **Staggered Incremental Hydration** (R142) in `SettingsComponents.kt`. Rendering `GuideSection` components sequentially with 60ms offsets smoothed out CPU spikes, eliminating ANRs on budget hardware. (R142)

---

## 🟢 Recently Resolved Issues (Aug.11.05)
*   **[Issue #140] [Severity: Medium] [Category: Performance] Automated Forensic Stress Test.**
    *   **Resolution**: Implemented a 5-second CPU/IO saturation routine in `TrackerService` triggered via the `PhoneSetupOverlay`. (R140).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.11.20)
