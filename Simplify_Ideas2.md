# 💡 Strategic Simplification Ideas (Batch 2) - Sep.23.01

## 🏛️ UI & State Architecture
1.  **Event Forwarding Delegate**: Create an `UiEventDelegate` to handle common navigation and settings draft events. Currently, `TrackerViewModel` and `ViewerViewModel` duplicate significant logic for `ToggleSettings`, `UpdateDraft*`, and `CommitSettings`. Consolidating this would reduce the lines of code in feature ViewModels by ~20%.
2.  **Shared Overlay Scope**: Move all shared overlays (Settings, Log, Ribbons) into a dedicated `OverlayHost` component that interacts only with `MainViewModel`. This would eliminate the need to pass extensive callback lists down to `TrackerScreen` and `ViewerScreen`.
3.  **Reactive Siren Lockout**: Move the siren cooldown/lockout logic from `AudioSynthesizer` into a `SirenUseCase`. This keeps the synthesizer focused purely on signal generation and allows the domain layer to manage temporal constraints.
