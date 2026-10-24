git add .
git commit -m "Remediation: Resolved ISSUE-OCT23.101-F6 and ISSUE-OCT24.00-F5. Implemented offline violation event sync and aligned versioning to Oct24.2. Oct24.2."
git tag -a Oct24.2 -m "Oct24.2 Remediation Baseline"
git push origin Oct24.2

# Project Issues & Hardening Tracking (Rigorous Audit) - Oct24.2

## 🎯 Current Resumption Focus: Remediation of SIMP-OCT22.0-S1 (Settings Validation Centralization).

## 🔴 Open Gaps & Unfinished Integration Points
*   **SIMP-OCT22.0-S1: Settings Validation Centralization (Medium).** Move identity uniqueness and regex validation to a pure `ConfigValidator`.

---

## 💡 Strategic Simplification Ideas (Ideas: 80)
*   **SIMP-OCT24.2-S1: Room Migration Unit Testing (Low).** Implement explicit migration tests for v87 -> v88 to verify `pending_violations` schema integrity automatically.
*   **SIMP-OCT23.121-S1: Violation Event Buffering (Medium).** (Implemented Oct24.2 - Archive candidate).
*   **SIMP-OCT23.120-S1: Database Schema Migration Automation (Low).** Explore automated Room schema export verification to prevent manual SQL drift during forensic field expansion.
*   **SIMP-OCT23.109-S1: Atomic Telemetry Accessors (Low).** Refactor `ConnectivitySuite` to use `AtomicReference` for all transient peer states to further reduce boilerplate in atomic update blocks.
*   **SIMP-OCT23.108-S1: Protobuf Mapping Boilerplate Reduction (Medium).** Utilize a generated mapping layer or generic field visitor to reduce the manual `hasField()` boilerplate in `TelemetryMapper.kt`.
*   **SIMP-OCT23.107-S1: Signaling Refresh Strategy Pattern (Medium).** Encapsulate delta-reset logic (spatial jump vs. temporal/packet frequency) into a dedicated `SignalingStrategy` to decouple `TelemetryProtobufMapper` from specific optimization thresholds.
*   **SIMP-OCT23.106-S1: Unified Telemetry Property Resolver (Medium).** Create a generic property resolver for `LocationUpdate` that handles both Protobuf and JSON optional values with a single authoritative fallback policy to eliminate manual ternary drift.
*   **SIMP-OCT23.101-S1: Telemetry Merger Polymorphism (High).** Replace the manual field-by-field merge in `TelemetryMerger.kt` with a reflection-free metadata-aware visitor to ensure all forensic fields are automatically included in `preferCached` blocks.
*   **SIMP-OCT24.0-S1: Signaling Validator discovery logic simplification (Medium).** Relax `isDiscoveryMode` to allow asymmetric pairing when one peer is using a default ID.
*   **SIMP-OCT23.0-S1: Forensic Snapshot Consolidation (Low).** Consolidate redundant `ForensicSnapshot` class definitions between `core:engine` and `HardwareSuite.kt` into a single shared domain model.
*   **SIMP-OCT23.100-S1: DialogHost Refactoring (Low).** Further simplify `MainAppContent.DialogHost` by utilizing a unified overlay/dialog state machine to reduce conditional nesting.

---

## ✅ Resolved Traceability & Metadata Issues
*   **ISSUE-OCT23.101-F6: Offline Violation Event Sync.** Resolved Oct24.2. Implemented persistent buffering for `violation_event` signaling in `OfflineRepository` and `ConnectivitySuite`. Discrete violations are now captured offline and synchronized upon recovery. (SOT ID 824).
*   **ISSUE-OCT24.00-F5: Version Alignment Failure.** Resolved Oct24.2. Aligned `BuildConfig.VERSION_NAME` and `MainViewModelMappingTest` to `Oct24.2`.
*   **ISSUE-OCT23.101-F4: Violation Persistence Integrity Gaps.** Resolved Oct23.121. Synchronized `isJammingCandidate` across violation reporting, signaling (JSON), and persistence layers (Room) for full forensic parity. (SOT ID 823).
*   **ISSUE-OCT23.101-F3: Missing Stationary Duration Propagation.** Resolved Oct23.120. Synchronized `stationaryDurationMs` across Protobuf schemas, TelemetryMapper (JSON/Proto), Room entities, and Database migrations. (SOT ID 822).
*   **ISSUE-OCT23.102-F9: Telemetry Flyweight Race Condition.** Resolved Oct23.109. Replaced shared flyweights in `ConnectivitySuite.kt` with local instances and atomic references to ensure concurrency safety. (SOT ID 821).
*   **ISSUE-OCT23.102-F8: Protobuf Default Value Pollution.** Resolved Oct23.108. Applied `optional` keyword to all scalar fields in `app_settings.proto` and hardened `TelemetryMapper.kt` with `hasField()` checks to prevent state regression. (SOT ID 820).
*   **ISSUE-OCT23.102-F7: Delta Compression Drift Prevention.** Resolved Oct23.107. Implemented periodic absolute coordinate refresh (every 50 packets) in `TelemetryProtobufMapper` (R-ID 625) to prevent permanent drift. (SOT ID 819).
*   **ISSUE-OCT23.101-F2: JSON Mapping State Overwrite.** Resolved Oct23.106. Fixed `mapJsonToStatus` and `mapJsonToSnapshot` in `TelemetryMapper.kt` to preserve current cached values (proximityCm, rtt, snapshots, etc.) when keys are missing in JSON. (SOT ID 818).
*   **ISSUE-OCT23.101-F1: Incomplete Telemetry Merge Logic.** Resolved Oct23.105. Updated `TelemetryMerger.kt` to merge all critical integrity flags, SIT metrics (F5), raw sensor values (F10), and proximity/index gaps (F11) during GPS outages. (SOT ID 813).
*   **SIMP-OCT22.10-S1: BackHandler Composition Local.** Resolved Oct23.100. Consolidated back button interception into `LocalBackAction` and `BackPressInterceptor` to reduce boilerplate across all overlays. (SOT ID 817).
*   **ISSUE-OCT24.00-F1: Build Stabilization (Infra).** Resolved Oct24.00. Corrected `activity-compose` dependency, upgraded Kotlin to 1.9.24 / JVM 17, and fixed Compose compiler compatibility. (SOT ID 816).

---

## 📊 Hardening Progress Dashboard
- **Oct24.2: [SOT Count: 824 (Rules: 334), Open: H:0, M:1, L:0, Ideas: H:3, M:30, L:47, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.121: [SOT Count: 823 (Rules: 333), Open: H:0, M:1, L:1, Ideas: H:3, M:29, L:47, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.120: [SOT Count: 822 (Rules: 332), Open: H:0, M:2, L:1, Ideas: H:3, M:29, L:46, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.109: [SOT Count: 821 (Rules: 331), Open: H:0, M:2, L:1, Ideas: H:3, M:29, L:45, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.108: [SOT Count: 820 (Rules: 330), Open: H:1, M:3, L:1, Ideas: H:3, M:29, L:44, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.107: [SOT Count: 819 (Rules: 329), Open: H:2, M:3, L:1, Ideas: H:3, M:28, L:44, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.106: [SOT Count: 818 (Rules: 328), Open: H:3, M:3, L:1, Ideas: H:3, M:27, L:44, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.105: [SOT Count: 817 (Rules: 327), Open: H:4, M:3, L:1, Ideas: H:3, M:26, L:44, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.102: [SOT Count: 817 (Rules: 327), Open: H:5, M:7, L:1, Ideas: H:3, M:26, L:44, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.101: [SOT Count: 817 (Rules: 327), Open: H:2, M:4, L:1, Ideas: H:3, M:26, L:44, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.100: [SOT Count: 817 (Rules: 327), Open: H:0, M:1, L:0, Ideas: H:2, M:26, L:44, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct24.00: [SOT Count: 816 (Rules: 326), Open: H:0, M:1, L:1, Ideas: H:2, M:26, L:43, Testing: 11 (Sub-items: 45), QA: 385]**
- **Oct23.00: [SOT Count: 814 (Rules: 324), Open: H:0, M:1, L:1, Ideas: H:2, M:25, L:43, Testing: 11 (Sub-items: 45), QA: 385]**
