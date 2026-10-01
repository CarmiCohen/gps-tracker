# SOT Master Requirements & Hardening Status (Oct.1.7)

## 🏗️ Architectural Master Rules (94 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity)
*   **1.72 Trail Telemetry Age Dimming (R338/S572)**: Map trails MUST reactively dim (transition to `Slate500`) if the point's telemetry age exceeds 35,000ms. This ensures visual distinction between active movement and stale positioning during connectivity stalls (Issue #MAP-SOT-02).
*   **1.73 Anchor Lock Visual Feedback (R018/S573)**: The map UI MUST display a persistent "ANCHOR LOCKED" badge when the `isAnchorLocked` flag is true. This badge MUST utilize orientation-aware padding to avoid occlusion by the Map Settings Toggle (Issue #MAP-SOT-03).
*   **1.74 Snapshot-Aware Marker Pooling (R147/S574)**: All imperative map marker and polyline pools in `MapOverlayManager` MUST utilize `SnapshotStateList` (`mutableStateListOf`) instead of legacy `ArrayList`. This ensures Compose snapshot integrity and prevents concurrent modification exceptions during high-frequency telemetry updates (Issue #MAP-SOT-01).
*   **1.75 Coupled Alarm Signaling (R042/S576)**: The system MUST NOT trigger physical audio (siren) without an accompanying high-priority notification and visual context. Connectivity alerts (Signal Loss, GPS Stall) are notification-only. (Oct.1.6).
*   **1.76 System-Wide Alarm Overlay (R578)**: Critical theft alarms MUST utilize `SYSTEM_ALERT_WINDOW` (Draw over other apps) to guarantee UI promotion when the device is unlocked and the app is backgrounded. This bypasses OEM-specific background activity start restrictions. (Oct.1.7).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 578**: System-Wide Alarm Overlay - Implemented `AlarmOverlayService` for guaranteed alert visibility. (Resolved Oct.1.7).
*   **SOT ID 577**: Ribbon Legibility Hardening - Adjusted offsets and tick alignment. (Resolved Oct.1.6).
*   **SOT ID 576**: Coupled Alarm Signaling - Decoupled sirens from connectivity alerts. (Resolved Oct.1.6).
*   **SOT ID 575**: Manual Silence Persistence - Enforced 5m duration via SirenLockoutUseCase. (Resolved Oct.1.6).
*   **SOT ID 574**: Snapshot-Aware Marker Pooling - Migrated `MapOverlayManager` pools to `SnapshotStateList`. (Resolved Oct.1.5).
*   **SOT ID 573**: Anchor Lock Visual Feedback - Implemented reactive `AnchorLockedBadge` in `AppMapContainer`. (Resolved Oct.1.5).
*   **SOT ID 572**: Trail Telemetry Age Dimming - Injected freshness checks into `UiStateCoordinator.computeTrailSegments`. (Resolved Oct.1.5).

---

## 🏁 Verification Chapters
*   **Chapter 31.205 (Overlay Hardening Audit)**: PASSED - Verified `AlarmOverlayService` promotes UI over third-party apps on Xiaomi/Samsung while device is unlocked. (Oct.1.7)
*   **Chapter 31.204 (Alarm signaling Hardening Audit)**: PASSED - Verified sirens no longer trigger on SIGNAL_LOSS; verified 5m silence persistence. (Oct.1.6)
*   **Chapter 31.203 (Map SOT Hardening Audit)**: PASSED - Verified marker pooling stability under heavy load, trail dimming at 35s threshold, and anchor badge visibility. (Oct.1.5)
*   **Chapter 31.201 (Thermal Persistence Audit)**: PASSED - Verified COOLING_MODE and cooling EnteredRt are correctly recovered after service restart during simulated heat event. (Oct.1.3)
