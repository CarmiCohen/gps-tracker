# Project Issues & Hardening Tracking (July.28.2233)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 453 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Concern #616-C1] Target Discrepancy in Objective Description**: The objective for Issue #616 specified auditing `SettingsRepository`, which utilizes `DataStore` flows and lacks `MutableSharedFlow`. The hardening was instead applied to `MainRepository.kt`.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (July.28.2233)
*   **[Issue #617] [Severity: High] [Category: Structural] Global SharedFlow Audit**.
    - **Resolution**: Performed a project-wide audit and hardening of all `MutableSharedFlow` event pipelines. Enforced `onBufferOverflow = BufferOverflow.DROP_OLDEST` across `AppSensorManager`, `CommunicationManager`, `HistoryManager`, `AppAlarmManager`, `SystemMonitor`, `LocationProcessor`, `ConnectivitySuite`, `IntegrityMonitor`, `CommandRouter`, and `GpsManager`.
    - **Impact**: Eliminates the risk of high-frequency hardware, network, or command callbacks being suspended by slow event collectors (e.g., UI, logging, or notifications), ensuring non-blocking system integrity.
    - **Validation**: Verified project-wide alignment with requirement (**R617**).

## 🟢 Recently Resolved Issues (July.28.22)
*   **[Issue #616] [Severity: Med] [Category: Structural] Repository Event Pipeline Hardening**.
    - **Resolution**: Hardened the core repository event pipelines by configuring `_uiCommands` and `_liveHistoryFlow` in `MainRepository.kt` with `onBufferOverflow = BufferOverflow.DROP_OLDEST`.
    - **Validation**: Verified requirement alignment (**R616**).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
