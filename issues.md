# Project Issues & Hardening Tracking (July.22.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 324 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #125: Baseline Verification**: A full clean build is required to ensure no residual circularities exist in the generated Dagger graph after the AppContainer removal.
*   **Issue #121: Provider Latency**: Circularity resolution via `Provider<T>` is stable but introduces minor lookup overhead in `LogManager`.
*   **Issue #120b: Budget Hardware Initialization Spikes**: Budget devices (A15) remain sensitive. The 500ms staggered startup is critical.

---

## 🔴 Open Issues
### Issue #113: R405c Fallback Efficacy Verification (Samsung A15)
*   **Description**: Perform long-term field testing on SM-A155F to confirm Accelerometer-based pulse prevents OS-level eviction.

---

## 🟢 Recently Resolved Issues (July.22.05)
*   **Issue #124: Hilt Migration Completion & AppContainer Decommissioning**.
    *   **Resolution**: Fully migrated all services, activities, repositories, and managers to Hilt. Removed manual DI logic from `GpsApplication` and `BaseMonitorService`.

## 🟢 Recently Resolved Issues (July.22.04)
*   **Issue #511: DataStore Singleton Violation**.
    *   **Resolution**: Refactored `SettingsRepository` to use a single `DataStore` instance via `Context` extension delegate. This ensures that even during the Hilt transition, multiple repository instances share the same underlying `DataStore` connection, preventing `IllegalStateException`.
