# Handover Document - v8.9.37 Hardening

## 🛠 Recent Modifications

### App Module (`:app`)
*   **TrackerService.kt (v8.9.37)**:
    *   **Issue #148**: Integrated `ServiceBehaviorUseCase.calculateGpsInterval` into the core tick loop. Enforces `A15_STABLE_GPS_POLLING_MS` (1000ms) for Samsung A15 devices.
    *   **Issue #148**: Implemented "Keep-Alive" WakeLock renewal on every service tick for A15 devices to prevent aggressive OEM background GNSS suspension.
    *   **Issue #191**: Applied `MUZZLE_HYSTERESIS_A15_MS` (500ms) to the sync muzzle release logic. This protects the virtual proximity sensor from optical flutter caused by screen/LED activity during network I/O.
*   **LogRepository.kt (v8.9.37)**:
    *   **Issue #241**: Ensured `snrSnapshot` and `vibeSnapshot` parity in local database persistence and retrieval. 
*   **OverlayComponents.kt (v8.9.36)**:
    *   **Issue #226**: Added `locationPendingReason` display to `LegacyDashboardGrid`.
*   **SharedUiComponents.kt (v8.9.36)**:
    *   **Issue #226**: Updated `StatusRowData` to display `locationPendingReason` in the Status Bar.
*   **SyncManager.kt (v8.9.36)**:
    *   **Issue #244**: Implemented offline buffering fallback in `pushCurrentStatus`.
*   **Database.kt**:
    *   **Migration 44 -> 45**: Added `locationPendingReason` to `PendingStatusEntity`.

## ✅ Resolved Issues (Ready for Audit)
*   **Issue #148**: Samsung A15 GPS Stalling remediation. (Complete)
*   **Issue #191**: Samsung A15 Proximity Flutter remediation. (Complete)
*   **Issue #241**: Forensic Log Enrichment Parity. (Complete)
*   **Issue #226**: Intelligent Uncertainty UX Mapping. (Complete)
*   **Issue #244/245**: SIT Rising-Edge & Offline Context. (Complete)
*   **Issue #214**: Unified Accuracy Fallback Logic. (Complete)
*   **Issue #403**: Network Integrity & Timeout Scaling. (Complete)
*   **Issue #436**: Shadow Constants Remediation. (Complete)

## ⚠️ Pending Validation
*   Hardware verification of A15 GPS stability over a 4-hour background session.
*   Verification of zero false proximity tamper alerts on A15 during high-frequency sync.
*   Verify `snrSnapshot` and `vibeSnapshot` are present in historical logs after a database reload.
