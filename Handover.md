# Forensic Handover (Oct.1.5 - UI HARDENING)

## 🎯 Current System State
*   **Version**: `Oct.1.5` | **Status**: HARDENED (Map & UI).
*   **Map Engine**:
    *   **Marker Pooling**: Migrated to `SnapshotStateList` for Compose stability.
    *   **Trail Dimming**: Active (35s stale threshold).
    *   **Anchor Badge**: Reactive implementation verified.
*   **Versioning**: Promptly visible in `app/build.gradle` and UI manifests.

## 🔴 Open Gaps (High Priority)
*   **Issue #1402-B (System Overlay)**: Red Alert lacks `SYSTEM_ALERT_WINDOW` implementation for backgrounded states.
*   **Issue #1409 (Siren Mismatch)**: Reports of sirens triggering without Red Screen context for connectivity events.
*   **Issue #1410 (Viewer Persistence)**: Remote alarms ignoring global mute/ack rules in specific race conditions.
*   **Issue #1412 (Ribbon Occlusion)**: Time scale legibility issues and drawing overlaps.

## 🚀 Resumption Focus: Forensic Overlays
*   **Immediate Path**: 
    1.  Implement system-level overlay for Red Alert (Issue #1402-B).
    2.  Audit `AppAlarmManager` vs `MainViewModel` for siren/context mismatch (#1409).
    3.  Refine Ribbon drawing offsets in `ForensicRibbon.kt`.

---

## 📊 Hardening Progress Dashboard (Oct.1.5)
- **Status**: [SOT Count: 243 (Rules: 92), Open: H:4, M:0, L:0, Ideas: H:0, M:1, L:1, Testing: 10, QA: 335]
- **Audit Record**: Map SOT Hardening completed; stale dimming and anchor badge functional.
