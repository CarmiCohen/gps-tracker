# SOT Master Requirements & Hardening Status (Oct.1.5)

## 🏗️ Architectural Master Rules (87 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity)
*   **1.72 Trail Telemetry Age Dimming (R338/S572)**: Map trails MUST reactively dim (transition to `Slate500`) if the point's telemetry age exceeds 35,000ms. This ensures visual distinction between active movement and stale positioning during connectivity stalls (Issue #MAP-SOT-02).
*   **1.73 Anchor Lock Visual Feedback (R018/S573)**: The map UI MUST display a persistent "ANCHOR LOCKED" badge when the `isAnchorLocked` flag is true. This badge MUST utilize orientation-aware padding to avoid occlusion by the Map Settings Toggle (Issue #MAP-SOT-03).
*   **1.74 Snapshot-Aware Marker Pooling (R147/S574)**: All imperative map marker and polyline pools in `MapOverlayManager` MUST utilize `SnapshotStateList` (`mutableStateListOf`) instead of legacy `ArrayList`. This ensures Compose snapshot integrity and prevents concurrent modification exceptions during high-frequency telemetry updates (Issue #MAP-SOT-01).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 574**: Snapshot-Aware Marker Pooling - Migrated `MapOverlayManager` pools to `SnapshotStateList`. (Resolved Oct.1.5).
*   **SOT ID 573**: Anchor Lock Visual Feedback - Implemented reactive `AnchorLockedBadge` in `AppMapContainer`. (Resolved Oct.1.5).
*   **SOT ID 572**: Trail Telemetry Age Dimming - Injected freshness checks into `UiStateCoordinator.computeTrailSegments`. (Resolved Oct.1.5).
*   **SOT ID 571**: Unified Alarm Authority Parity - Synced MainRepository acknowledgment flow with VR_ partition. (Resolved Oct.1.3).
*   **SOT ID 570**: Descending Prefix Resolution - Enforced length-priority in AppRole fromKey mapping. (Resolved Oct.1.3).
*   **SOT ID 569**: Root-Level Thermal Persistence - Integrated cooling state into root DataStore schema. (Resolved Oct.1.3).

---

## 🏁 Verification Chapters
*   **Chapter 31.203 (Map SOT Hardening Audit)**: PASSED - Verified marker pooling stability under heavy load, trail dimming at 35s threshold, and anchor badge visibility. (Oct.1.5)
*   **Chapter 31.201 (Thermal Persistence Audit)**: PASSED - Verified COOLING_MODE and coolingEnteredRt are correctly recovered after service restart during simulated heat event. (Oct.1.3)
*   **Chapter 31.202 (Prefix Isolation Audit)**: PASSED - Verified AppRole.fromKey correctly distinguishes between "V_TEST" and "VR_TEST" using length-descending evaluation. (Oct.1.3)
