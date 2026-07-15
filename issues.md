# Project Issues & Hardening Tracking (July.1.12)

This document tracks active issues, technical debt, and pending implementation tasks.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 286 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Manual Override Persistence (Issue #502)**: The manual override for hardware configuration status must be rigorously verified against process death to prevent transient "Hardware Config Incomplete" alarms during background service restoration.
*   **EMA Tuning (Issue #504)**: The alpha values for position smoothing (`POSITION_EMA_ALPHA_DEFAULT = 0.3`) may require field verification in high-multipath environments (e.g., urban canyons) compared to the previous Kalman implementation.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (July.1.12)
*   **Issue #504 (R406d)**: Kalman Filter Removal.
    *   Deleted `ImmFilter.kt` logic.
    *   Implemented Exponential Moving Average (EMA) smoothing for coordinates, speed, and bearing in `LocationSentinel.kt`.
    *   Simplified `LocationProcessor.kt` by removing Kalman state management.
    *   Updated `EngineConstants.kt` with tunable EMA parameters.

*   **Issue #502 (R406b)**: Device Independency & Hardware Abstraction.
    *   Removed manufacturer-specific logic from the core engine.
    *   Introduced `HardwareCapabilities` abstraction.
    *   Genericized UI and notification handling for background restrictions.

*   **Issue #501 (R406a)**: Unified Heartbeat (2s Standard).
    *   Standardized all periodic tasks and hardware polling to 2000ms.
