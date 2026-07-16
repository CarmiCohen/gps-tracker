# Project Issues & Hardening Tracking (July.16.17)

This document tracks active issues, technical debt, and pending implementation tasks.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 287 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **AppContainer Initialization Order**: With the move to manual DI, circular dependencies between `LogManager` and `AppNetworkManager` are handled via `lazy` and lambdas. Future components must be added to `AppContainer` with careful consideration of the instantiation graph to avoid `UninitializedPropertyAccessException`.
*   **Manual Override Persistence (Issue #502)**: The manual override for hardware configuration status must be rigorously verified against process death to prevent transient "Hardware Config Incomplete" alarms during background service restoration.
*   **EMA Tuning (Issue #504)**: The alpha values for position smoothing (`POSITION_EMA_ALPHA_DEFAULT = 0.3`) may require field verification in high-multipath environments.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (July.16.17)
*   **Issue #503 (R406c)**: Hilt Removal & Manual DI Migration.
    *   Removed `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`, and `@Singleton` from the entire project.
    *   Implemented `AppContainer` in `GpsApplication` as the central DI registry.
    *   Transitioned `MainActivity` and `AlarmActivity` to manual `MainViewModelFactory`.
    *   Implemented manual `WorkerFactory` for `MaintenanceWorker` and `BootServiceStartWorker`.
    *   Purged `hilt-android-gradle-plugin` and all Dagger/Hilt dependencies from Gradle.

*   **Issue #504 (R406d)**: Kalman Filter Removal.
    *   Implemented Exponential Moving Average (EMA) smoothing for coordinates, speed, and bearing.

*   **Issue #502 (R406b)**: Device Independency & Hardware Abstraction.
    *   Introduced `HardwareCapabilities` abstraction.

*   **Issue #501 (R406a)**: Unified Heartbeat (2s Standard).
    *   Standardized all periodic tasks to 2000ms.
