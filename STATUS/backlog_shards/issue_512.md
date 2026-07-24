# Issue #512: Consolidate Sentinel Statuses

## 🎯 Status: Resolved (Historical)
**Category**: Engine / Refactoring

---

## 📝 Description
The tracking engine used multiple boolean flags (isJump, isJammer, isStalled) to track point validity, leading to complex and error-prone conditional logic. This task migrates the system to a unified `SentinelStatus` enum.

## 🛠️ Resolution
- Defined `SentinelStatus` enum with states: `VALID`, `JUMP`, `TAMPER`, `OUTLIER`, `JAMMER_SUSPICION`.
- Refactored `TelemetryMerger.kt` to process the single status field.
- Simplified UI binding logic by mapping the enum to specific visual indicators.

## 🔗 References
- **Requirement**: R502 (Status Authority)
- **File**: `core/engine/src/main/java/com/gps19/core/engine/TelemetryMerger.kt`
