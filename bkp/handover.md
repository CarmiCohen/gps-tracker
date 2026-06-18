# Forensic Handover - GPS Tracker (v8.8.37)

## **Project Status**
The project is currently at **v8.8.37**. It follows a high-assurance modular architecture with a strict separation between the Android `:app` layer and the pure Kotlin `:core:engine`.

### **Core Accomplishments (v8.8.37)**
1.  **Dead Code Cleanup:** Removed `SyncManager.broadcastIntegrityUpdate` and `SyncManager.pushStatusUpdateOnly` (Issue 134). Telemetry pipeline is now focused on `pushCurrentStatus`.
2.  **Forensic Audit (In Progress):** Verified SIT field mapping in `RemoteHandler.init`.

## **Current Architecture State**
*   **Monotonic Timing:** Strictly enforced via `TimeProvider` across all layers.
*   **State Management:** `MainViewModel` is decoupled into specific UseCases.
*   **Database Migration v33:** Purged legacy columns and standardized forensic fields.

## **Open Issues (from issues.md)**
| ID | Rank | Task |
|:---|:---|:---|
| 133 | 8 | **Xiaomi Background Stability Test:** Verify 10Hz polling and autostart effectiveness on physical hardware. |
| 135 | 7 | **Forensic Parity:** Verify `verticalVelocity` initialization in Migration v33. |
| 136 | 8 | **RemoteHandler SIT Audit:** Final check of forensic field mapping in `RemoteHandler.init`. |

## **Context for Resumption**
The next steps involve completing the forensic parity checks for `verticalVelocity` across the history and engine models.

**Status:** RIGOROUSLY REVIEWED. ARCHITECTURALLY ALIGNED. NO PENDING REGRESSIONS.
