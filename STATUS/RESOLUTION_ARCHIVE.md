# 🏛️ Resolution Archive - Oct.1.5

## 🏁 Issue #MAP-SOT-01/02/03: Map Engine Hardening (SOT Audit)
*   **Resolved**: Oct.1.5
*   **Root Cause**: 
    1.  Marker pooling used legacy `ArrayList`, risking Compose state desynchronization.
    2.  Trail segments lacked visual aging, violating R338 (freshness awareness).
    3.  Stationary Anchor lacked UI visibility.
*   **Remediation**: 
    *   **Marker Pooling**: Migrated `MapOverlayManager` to `SnapshotStateList` (`mutableStateListOf`).
    *   **Trail Freshness**: Injected telemetry age checks into `UiStateCoordinator.computeTrailSegments`; stale points (>35s) now dim to `Slate500`.
    *   **Anchor Feedback**: Integrated `AnchorLockedBadge` into `AppMapContainer` with reactive binding to `isAnchorLocked`.
*   **Significance**: High (UI/UX Integrity & Compose Stability).
*   **SOT ID**: 572, 573, 574

---

# 🏛️ Resolution Archive - Oct.1.3

## 🏁 Issue #1408: Thermal & Convergence Audit
*   **Resolved**: Oct.1.3
*   **Root Cause**: 
    1.  `COOLING_MODE` state was memory-only, causing forensic gaps and alert desynchronization after service restarts during thermal events.
    2.  `AppRole` prefix matching logic (`V_` vs `VR_`) was order-dependent and susceptible to collisions, risking state leakage between local settings and remote telemetry.
*   **Remediation**: 
    *   **Prefix Isolation**: Refined `AppRole.fromKey` to use length-descending evaluation, ensuring `VR_` (Remote) is matched before `V_` (Self).
    *   **Thermal Persistence**: Added `is_cooling_mode_active` and `cooling_entered_rt` to the root Protobuf schema and implemented recovery logic in `IntegrityMonitor`.
    *   **Authority Unification**: Updated `MainRepository` to use the `AppRole.VIEWER_REMOTE` partition for alarm acknowledgments in Viewer mode, aligning with `AppAlarmManager` logic.
*   **Significance**: High (Forensic Stability & Multi-Role Integrity).
*   **SOT ID**: 569, 570, 571

---

# 🏛️ Resolution Archive - Oct.1.2

## 🏁 Issue #1407: Unified Storage Authority
*   **Resolved**: Oct.1.2
*   **Root Cause**: Namespaced persistent storage operations relied on manual string concatenation of role prefixes (e.g., `role.prefix + KEY`), which was error-prone and bypassed type safety provided by the `AppRole` enum. A critical leakage was found where `CLOCK_DRIFT_REF_KEY` was read globally in `MonitorService`.
*   **Remediation**: 
    *   **Repository Overloads**: Refactored `SettingsRepository` and `MainRepository` to provide storage method overloads accepting `AppRole` as a primary parameter.
    *   **MonitorService Fix**: Resolved a critical leakage in `onServiceInitialize` where `CLOCK_DRIFT_REF_KEY` was being read from the global namespace instead of the role-partitioned storage.
    *   **Global Purge**: Finalized implementation by removing redundant global fields and manual routing fall-throughs from `SettingsRepository`, enforcing `AppRole` authority at the compiler level.
    *   **Namespace Integrity**: Eliminated manual prefixing across the codebase, ensuring all role-based state isolation follows a single, verified authority.
*   **Significance**: High (Structural Integrity).
*   **SOT ID**: 568 (Unified Storage Authority)

...
*(Full historical records maintained in SOT Archive)*
