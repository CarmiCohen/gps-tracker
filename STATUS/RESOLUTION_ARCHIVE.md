# 🏛️ Resolution Archive - Oct.1.6

## 🏁 Issue #1409/1410/1412: Alarm Signaling & UI Hardening
*   **Resolved**: Oct.1.6
*   **Root Cause**: 
    1.  Connectivity alerts (Signal Loss, GPS Stall) were incorrectly categorized as "Special", causing sirens and Red-Screen promotion without visual context.
    2.  Manual "Stop Siren" action used a short cooldown, allowing re-triggering under specific race conditions or high-frequency telemetry updates.
    3.  Ribbon time scales in `SharedUiComponents.kt` were occluded by drawings due to insufficient bottom-reserved space and tick-alignment jitter.
*   **Remediation**: 
    *   **Logic Isolation**: Hardened `isSpecialType` in `AppAlarmManager` to exclude connectivity events, making them notification-only.
    *   **Persistence**: Standardized manual silence to 5m duration (`SILENCE_TIMEOUT_MS`) via `SirenLockoutUseCase`.
    *   **UI/UX**: Standardized connection header to "CON"; increased ribbon baseline offsets and refined tick alignment logic for large-scale views (4H/24H/7D).
    *   **Reactive Promotion**: Implemented reactive notification summary updates in `AppEventCoordinator` to ensure full-screen intents are refreshed on alarm state changes.
*   **Significance**: High (System Integrity & UX Clarity).
*   **SOT ID**: 575, 576, 577

---

# 🏛️ Resolution Archive - Oct.1.5

## 🏁 Issue #MAP-SOT-01/02/03: Map Engine Hardening (SOT Audit)
*   **Resolved**: Oct.1.5
*   **Root Cause**: 
    1.  Marker pooling used legacy `ArrayList`, risking Compose desynchronization.
    2.  Trail segments lacked visual aging.
    3.  Stationary Anchor lacked UI visibility.
*   **Remediation**: 
    *   **Marker Pooling**: Migrated `MapOverlayManager` to `SnapshotStateList`.
    *   **Trail Freshness**: Injected telemetry age checks; stale points (>35s) now dim.
    *   **Anchor Feedback**: Integrated `AnchorLockedBadge`.
*   **Significance**: High.
*   **SOT ID**: 572, 573, 574

...
*(Full historical records maintained in SOT Archive)*
