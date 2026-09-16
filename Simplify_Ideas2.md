# Simplicity & Forensic Hardening Ideas (Phase 2)

## 💡 Architectural Refinements
1.  **Signaling Pipeline Abstraction**: Now that `PowerStateProvider` is abstracted, the entire signaling pipeline in `ConnectivitySuite` can be made testable by abstracting the socket layer, similar to how we handled power states (Sep.16.06).
2.  **Redundant Violation State**: Investigate if `SessionManager`'s violation state can be consolidated with `UnifiedPowerPolicy`'s deferral logic to reduce cross-component polling.

## 🛠️ Cleanup Tasks
1.  **Obsolete UiAutomator Dependency**: Once all shell-based environment simulations are removed, we can consider removing the `uiautomator` dependency from `app/build.gradle` to reduce test binary size.
