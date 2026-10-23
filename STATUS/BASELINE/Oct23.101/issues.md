git add .
git commit -m "Audit: Rigorously rechecked ISSUE-OCT22.0-F4 implementation. Identified 6 residual gaps and regressions. Version Oct23.101."
git tag -a Oct23.101 -m "Oct23.101 Audit Baseline"
git push origin Oct23.101

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct23.101

## 🎯 Current Resumption Focus: Remediation Hardening of ISSUE-OCT22.0-F4.

## 🔴 Open Gaps & Unfinished Integration Points
*   **ISSUE-OCT23.101-F1: Incomplete Telemetry Merge Logic (High).** `TelemetryMerger.kt` fails to merge critical integrity flags (`isJammer`, `isStalled`, `isBatteryLow`, etc.) when GPS is stale (`preferCached = true`), preventing Viewer awareness of these states during GPS outages.
*   **ISSUE-OCT23.101-F2: JSON Mapping State Overwrite (High).** `TelemetryMapper.kt` overwrites multiple fields with hardcoded defaults (e.g., `-1.0` for `proximityCm`, `0` for `rtt`) in `mapJsonToStatus` instead of preserving current cached values when keys are missing.
*   **ISSUE-OCT23.101-F3: Missing Stationary Duration Propagation (Medium).** `stationary_duration_ms` is serialized in `toMap()` but ignored in all deserialization (`mapJsonToStatus`, `mapProtoToStatus`) and merge (`TelemetryMerger`) paths.
*   **ISSUE-OCT23.101-F4: Violation Persistence Integrity Gaps (Medium).** `MainRepository.addViolation` and `ConnectivitySuite.emitViolation` fail to propagate or store the `isJammingCandidate` field added to `ViolationEntity` in v85.
*   **ISSUE-OCT23.101-F5: SIT Metric Parity Missing (Medium).** Suspicious Internal Trend (SIT) metrics (vertical velocity, sitBaro, etc.) are excluded from the `preferCached` merge block in `TelemetryMerger.kt`.
*   **ISSUE-OCT23.101-F6: Offline Violation Event Sync (Low).** Discrete violation markers emitted via `emitViolation` are real-time only and are not buffered or synchronized after recovery from an offline state.
*   **SIMP-OCT22.0-S1: Settings Validation Centralization (Medium).** Move identity uniqueness and regex validation to a pure `ConfigValidator`.

---

## 💡 Strategic Simplification Ideas (Ideas: 73)
*   **SIMP-OCT23.101-S1: Telemetry Merger Polymorphism (High).** Replace the manual field-by-field merge in `TelemetryMerger.kt` with a reflection-free metadata-aware visitor to ensure all forensic fields are automatically included in `preferCached` blocks.
*   **SIMP-OCT24.0-S1: Signaling Validator discovery logic simplification (Medium).** Relax `isDiscoveryMode` to allow asymmetric pairing when one peer is using a default ID.
*   **SIMP-OCT23.0-S1: Forensic Snapshot Consolidation (Low).** Consolidate redundant `ForensicSnapshot` class definitions between `core:engine` and `HardwareSuite.kt` into a single shared domain model.
*   **SIMP-OCT23.100-S1: DialogHost Refactoring (Low).** Further simplify `MainAppContent.DialogHost` by utilizing a unified overlay/dialog state machine to reduce conditional nesting.

---

## ✅ Resolved Traceability & Metadata Issues
*   **SIMP-OCT22.10-S1: BackHandler Composition Local.** Resolved Oct23.100. Consolidated back button interception into `LocalBackAction` and `BackPressInterceptor` to reduce boilerplate across all overlays. (SOT ID 817).
*   **ISSUE-OCT24.00-F1: Build Stabilization (Infra).** Resolved Oct24.00. Corrected `activity-compose` dependency, upgraded Kotlin to 1.9.24 / JVM 17, and fixed Compose compiler compatibility. (SOT ID 816).
*   **ISSUE-OCT24.00-F2: Signaling Discovery Logic.** Resolved Oct24.00. Relaxed `isDiscoveryMode` to allow default Trackers to adopt custom viewers during initial pairing. (SOT ID 815).
*   **ISSUE-OCT24.00-F3: Broadcast Export Security.** Resolved Oct24.00. Migrated all manual `registerReceiver` calls to `ContextCompat` with explicit export flags to satisfy Android 14 requirements. (SOT ID 816).
*   **ISSUE-OCT24.00-F4: Test Suite Freshness.** Resolved Oct24.00. Synchronized `MainViewModelMappingTest` version expectations and fixed Hilt instrumentation test dependencies. (SOT ID 816).
*   **ISSUE-OCT22.16-F1: Forensic trace raw sensor parity.** Resolved Oct23.00. Extended `ForensicSnapshot` and `LocationUpdate` with `lux` and `acoustic` fields for full raw sensor trace parity. (SOT ID 814).
*   **(Previous resolutions archived in Oct23.00 baseline).**

---

## 📊 Hardening Progress Dashboard
- **Oct23.101: [SOT Count: 817 (Rules: 327), Open: H:2, M:4, L:1, Ideas: H:1, M:2, L:1, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.100: [SOT Count: 817 (Rules: 327), Open: H:0, M:1, L:0, Ideas: H:0, M:1, L:0, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct24.00: [SOT Count: 816 (Rules: 326), Open: H:0, M:1, L:1, Ideas: H:0, M:1, L:0, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.00: [SOT Count: 814 (Rules: 324), Open: H:0, M:1, L:1, Ideas: H:0, M:1, L:0, Testing: 11 (Sub-items: 45), QA: 385]**
