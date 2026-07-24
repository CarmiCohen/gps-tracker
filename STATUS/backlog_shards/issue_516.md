# Issue #516: De-duplicate Status Logic

## 🎯 Status: Resolved (Historical)
**Category**: Architectural / Refactoring

---

## 📝 Description
The concept of "Status" was fragmented across multiple roles and components (Engine, UI, Repository). This task unified all status-related metadata into `SystemHealthState` to ensure consistent reporting.

## 🛠️ Resolution
- Defined `SentinelStatus` as the authoritative enum for point-level validity.
- Integrated `SystemHealthState` into `PersistencePolicy` to drive trail and history saving decisions.
- Removed redundant status flags from `MainUiState`.

## 🔗 References
- **Requirement**: R502 (Status Authority)
- **File**: `core/engine/src/main/java/com/gps19/core/engine/PersistencePolicy.kt`
