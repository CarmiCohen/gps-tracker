# Issue #520: Purge Signaling Command Leftovers

## 🎯 Status: Resolved (July.23.01)
**Category**: Code Cleanup / Signaling

---

## 📝 Description
The signaling system contained legacy command handlers and unused message types from the internal prototype phase that were cluttering the `CommandRouter` and `CommunicationManager`.

## 🛠️ Resolution
- Removed deprecated signaling constants from `SignalingConstants.kt`.
- Purged unused `UiCommand` types from `Models.kt`.
- Simplified `CommandRouter.startObservingCommands` by removing dead branches.

## 🔗 References
- **Requirement**: R520 (Signaling Purity)
- **Archive**: [RESOLUTION_ARCHIVE.md](../RESOLUTION_ARCHIVE.md)
