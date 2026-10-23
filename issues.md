git add .
git commit -m "Forensic Expansion: Raw sensor parity for lux and acoustic snapshots (ISSUE-OCT22.16-F1). Version Oct23.00."
git tag -a Oct23.00 -m "Forensic Trace Raw Sensor Parity"
git push origin Oct23.00

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct23.00

## 🎯 Current Resumption Focus: UI UX Simplification.

## 🔴 Open Gaps & Unfinished Integration Points
*   **SIMP-OCT22.10-S1: BackHandler Composition Local (Low).** Consolidate back button interception into a reusable composition local or custom modifier.
*   **SIMP-OCT22.0-S1: Settings Validation Centralization (Medium).** Move identity uniqueness and regex validation to a pure `ConfigValidator`.

---

## 💡 Strategic Simplification Ideas (Ideas: 70)
*   **SIMP-OCT23.0-S1: Forensic Snapshot Consolidation (Low).** Consolidate redundant `ForensicSnapshot` class definitions between `core:engine` and `HardwareSuite.kt` into a single shared domain model.
*   **SIMP-OCT22.10-S1: BackHandler Composition Local (Low).** Consolidate back button interception into a reusable composition local or custom modifier.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT22.16-F1: Forensic trace raw sensor parity.** Resolved Oct23.00. Extended `ForensicSnapshot` and `LocationUpdate` with `lux` and `acoustic` fields for full raw sensor trace parity. (SOT ID 814).
*   **ISSUE-OCT22.16-F2: Protobuf Schema Forensic expansion.** Resolved Oct23.00. Synchronized `lux_snapshot` and `acoustic_snapshot` fields in `RealtimeStatus` and `TrackerStatusProto`.
*   **ISSUE-OCT22.16-F3: Database Migration 85->86.** Resolved Oct23.00. Verified migration adding `luxSnapshot` and `acousticSnapshot` columns to `LogEntity`, `HistoryEntity`, and `PendingStatusEntity`.
*   **ISSUE-OCT22.0-F4: Telemetry Sync & Violation Propagation Hardening.** Resolved Oct22.16. (SOT ID 813).
*   **(Previous resolutions archived in Oct22.15 baseline).**

---

## 📊 Hardening Progress Dashboard
- **Oct23.00: [SOT Count: 814 (Rules: 324), Open: H:0, M:1, L:1, Ideas: H:2, M:25, L:43, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct22.16: [SOT Count: 813 (Rules: 323), Open: H:0, M:1, L:2, Ideas: H:2, M:25, L:42, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct22.15: [SOT Count: 813 (Rules: 323), Open: H:0, M:0, L:0, Ideas: H:2, M:25, L:42, Testing: 11 (Sub-items: 45), QA: 385]**
