# Forensic Handover (Oct.2.9 - TRACKERSTATUS CONVERGENCE)

## 🎯 Current System State
*   **Version**: `Oct.2.9` | **Status**: MID-SURGERY (Issue #1314 Convergence).
*   **Issue #1314: TrackerStatus Convergence**:
    *   **Consolidation**: Successfully purged the `TrackerStatus` DTO from `Models.kt`. `LocationUpdate` (in `core:engine`) is now the unified monolith for telemetry, persistence, and signaling.
    *   **UI Alignment**: `MainViewModel`, `MainUiState`, and `ConnectivitySuite` have been migrated to the new monolith.
    *   **Persistence**: `SettingsMapper` and `TelemetryProtobufMapper` are updated to map directly to/from `LocationUpdate`.
    *   **Engine**: `MonitorService` and `TelemetryMapper` are transitioned to use the unified flyweights.

## 🔴 Critical Gaps & Build Failures
*   **Build Status**: 🔴 **FAILING** (app:assembleDebug).
*   **Mutability Conflict (`LocationUpdate.kt`)**: Several evaluation flags (e.g., `isJammer`, `isStalled`, `tamperDetected`) were implemented as read-only `val` getters in `LocationUpdate`. However, `MonitorService` and `TelemetryMapper` attempt to write to these directly on the flyweight.
    *   *Forensic Note*: These should either be converted to `var` properties that delegate to the underlying `.integrity` / `.kinetic` state, or call sites must be updated to target the partitioned state objects directly.
*   **Routing Errors**: 
    *   `TelemetryMapper.kt`: `liftIdx` is unresolved in `mapStatusToPending` because it was incorrectly routed to `status.integrity.liftIdx`. It should be `status.liftIdx` (via getter) or `status.atmospheric.liftIdx`.
    *   `MonitorService.kt`: Assignment errors on lines 486-491 due to the `val` getters mentioned above.
    *   `MainFileHelper.kt`: Unresolved reference `loadTrackerState` on line 391. This was likely a repository method renamed or removed during the `TrackerStatus` purge.
*   **ProGuard**: `proguard-rules.pro` has been updated to keep `LocationUpdate` and remove the obsolete `TrackerStatus` rule.

## 🚀 Resumption Action Path
1.  **Fix `LocationUpdate.kt`**: Change convenience properties from `val` getters to `var` with custom setters, OR update `MonitorService.kt` and `TelemetryMapper.kt` to write to `integrity` and `kinetic` objects.
2.  **Restore Build**:
    *   Fix assignments in `MonitorService.processTick` (lines 486-491).
    *   Fix property routing in `TelemetryMapper.mapStatusToPending` (change `integrity.liftIdx` to `atmospheric.liftIdx`).
    *   Update `MainFileHelper.kt` to use `MainRepository.getTrackerState()` instead of `loadTrackerState()`.
3.  **Final Cleanup**: Verify `SettingsMapper.kt` parameter names match the `IntegrityState` and `AtmosphericState` constructors.
4.  **Verification**: Execute `./gradlew app:assembleDebug` to confirm architectural consolidation.

---

## 📊 Hardening Progress Dashboard (Oct.2.9)
- **Status**: [SOT Count: 254 (Rules: 111), Open Issues: H:1 (Build Restoration), M:1, L:0]
- **Audit Record**: `TrackerStatus` DTO fully purged; Monolith established; Build broken during property mutability shift.
