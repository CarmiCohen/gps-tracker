# Hardening Phase: Primary Tracking Document (v8.9.42)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[compliance.md](compliance.md)** archive.

**For historical resolutions, see [issues_archive.md](issues_archive.md).**

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 1 |
| **Validation Tasks** | 🟡 Pending Hardware | 0 |
| **Resolved (this phase)** | 🟢 Archived | 93 |

---

## 🔴 Open Issues (Contradictions & Inconsistencies)
*   **Issue #365: UI Inconsistency - Local Ghost Mode Suppression in StatusBar**: In `SharedUiComponents.kt`, the `StatusBar` component forces `isTelemetryFresh` to `true` for the primary status row when the app is in `tracker` mode. This prevents "Ghost Mode" dimming for local sensors even if they are stale, violating the Unified UI Staleness objective (#338).

---

## 🟡 Open Issues (Hardening & Future Roadmap)
*   **Issue #366: Resilience Hardening - High-Resilience Watchdog & Battery Redundancy**: Implement a Dual-Service Watchdog using `AlarmManager.setExactAndAllowWhileIdle` and a `WorkManager` heartbeat to ensure service persistence in Doze mode. **Objective**: Make the manual "Ignore Battery Optimizations" setup process redundant or optional by ensuring silent recovery and high-resilience persistence across OS-level kills.

---

## 🟢 Resolved (this phase)
*   **Issue #364: Logic Error - GPS Freshness coupled to Telemetry Pulse**: (Fixed v8.9.42) Decoupled GPS health from the general telemetry heartbeat in `DashboardUseCase.kt` and `GlobalStatusBar`. GPS status now utilizes `loc.timestamp` (position age) exclusively to determine visual staleness, preventing vibration/sensor updates from masking stale coordinates.
*   **Issue #360: Logic Alignment - Jump Engine Threshold**: Replaced hardcoded `5.0` m/s in `PhysicsUtils.kt` with `JUMP_GATE_SENSOR_MISMATCH_MPS` (10.0 m/s) for consistency.
*   **Issue #361: Documentation Refactor - ID Migration**: Bulk migrated legacy references (#214, #218, #273) to Authoritative IDs (#325, #190, #315) across all `/DOCS` files.
*   **Issue #362: Regression Fix - Xiaomi Override Key Typo**: Fixed unresolved reference in `MainViewModel.kt` by correcting `IS_XIAOMI_MANUAL_OVER_RIDE_KEY` to `IS_XIAOMI_MANUAL_OVERRIDE_KEY` in `MainRepository.kt`. (Resolved during v8.9.42 resumption)
*   All other items from this phase have been moved to **[issues_archive.md](issues_archive.md)** for audit trail preservation.
