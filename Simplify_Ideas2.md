# 💡 Strategic Simplification Ideas (Batch 2) - Sep.23.03

## 🏛️ UI & State Architecture
1.  **Issue #1200: Shared Overlay Scope (High Priority)**: Move all shared overlays (Settings, Log, Ribbons, GNSS Detail) into a dedicated `OverlayHost` component that interacts only with `MainViewModel`. Currently, `TrackerScreen` and `ViewerScreen` still manually manage overlay visibility and callback routing. A centralized host would eliminate the need to pass extensive callback lists down to feature screens and reduce UI boilerplate by ~15%.
2.  **Reactive Siren Lockout**: Move the siren cooldown/lockout logic from `AudioSynthesizer` into a `SirenUseCase`. This keeps the synthesizer focused purely on signal generation and allows the domain layer to manage temporal constraints.
3.  **UI Event Routing Unification**: Now that draft handling is centralized in `MainViewModel`, refactor the remaining feature-specific navigation events into a single `UiEventDelegate` or `NavigationCoordinator`. This would further decouple `MainViewModel` from specific UI implementation details.
4.  **Hilt ViewModel Scope Optimization**: Evaluate if `TrackerViewModel` and `ViewerViewModel` can be scoped to the navigation backstack entry rather than the standard hilt scope to ensure state is cleanly wiped when exiting a role, without relying on manual `reset()` calls.

## ✅ Implemented Simplifications
*   **Settings Draft Consolidation (Sep.23.03)**: Successfully moved `UpdateDraft*`, `CommitSettings`, and draft preparation logic from feature ViewModels to `MainViewModel`. This eliminated ~200 lines of redundant code and resolved the UI input lock issue.
