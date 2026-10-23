git add .
git commit -m "Build Stabilization: Resolved dependency mismatches, lint violations, and test regressions. Version Oct24.00."
git tag -a Oct24.00 -m "Oct24.00 Stabilization Release"
git push origin Oct24.00

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct24.00

## 🎯 Current Resumption Focus: UI UX Simplification.

## 🔴 Open Gaps & Unfinished Integration Points
*   **SIMP-OCT22.10-S1: BackHandler Composition Local (Low).** Consolidate back button interception into a reusable composition local or custom modifier.
*   **SIMP-OCT22.0-S1: Settings Validation Centralization (Medium).** Move identity uniqueness and regex validation to a pure `ConfigValidator`.

---

## 💡 Strategic Simplification Ideas (Ideas: 71)
*   **SIMP-OCT24.0-S1: Signaling Validator discovery logic simplification (Medium).** Relax `isDiscoveryMode` to allow asymmetric pairing when one peer is using a default ID.
*   **SIMP-OCT23.0-S1: Forensic Snapshot Consolidation (Low).** Consolidate redundant `ForensicSnapshot` class definitions between `core:engine` and `HardwareSuite.kt` into a single shared domain model.
*   **SIMP-OCT22.10-S1: BackHandler Composition Local (Low).** Consolidate back button interception into a reusable composition local or custom modifier.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT24.00-F1: Build Stabilization (Infra).** Resolved Oct24.00. Corrected `activity-compose` dependency, upgraded Kotlin to 1.9.24 / JVM 17, and fixed Compose compiler compatibility. (SOT ID 816).
*   **ISSUE-OCT24.00-F2: Signaling Discovery Logic.** Resolved Oct24.00. Relaxed `isDiscoveryMode` to allow default Trackers to adopt custom viewers during initial pairing. (SOT ID 815).
*   **ISSUE-OCT24.00-F3: Broadcast Export Security.** Resolved Oct24.00. Migrated all manual `registerReceiver` calls to `ContextCompat` with explicit export flags to satisfy Android 14 requirements. (SOT ID 816).
*   **ISSUE-OCT24.00-F4: Test Suite Freshness.** Resolved Oct24.00. Synchronized `MainViewModelMappingTest` version expectations and fixed Hilt instrumentation test dependencies. (SOT ID 816).
*   **ISSUE-OCT22.16-F1: Forensic trace raw sensor parity.** Resolved Oct23.00. Extended `ForensicSnapshot` and `LocationUpdate` with `lux` and `acoustic` fields for full raw sensor trace parity. (SOT ID 814).
*   **(Previous resolutions archived in Oct23.00 baseline).**

---

## 📊 Hardening Progress Dashboard
- **Oct24.00: [SOT Count: 816 (Rules: 326), Open: H:0, M:1, L:1, Ideas: H:2, M:26, L:43, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.00: [SOT Count: 814 (Rules: 324), Open: H:0, M:1, L:1, Ideas: H:2, M:25, L:43, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct22.16: [SOT Count: 813 (Rules: 323), Open: H:0, M:1, L:2, Ideas: H:2, M:25, L:42, Testing: 11 (Sub-items: 45), QA: 385]**
