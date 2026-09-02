# Simplicity Ideas 2 (Sep.02.43)

*   **Idea #238**: Unify `LocationUpdate` (Engine) and `LocationState` (UI). Currently, `TelemetryUseCase` manually maps ~50 fields between these structures. Merging them into a single `@Serializable` class used by both layers would eliminate the mapping boilerplate and reduce allocation churn during high-frequency GPS ticks (R3.1).
*   **Idea #239**: Consolidate `localLocation` and `trackerLocation` into a single `PeerGroup` flow in `MainRepository`. This would allow `MainViewModel` to subscribe to one stream and filter internally, reducing the number of active coroutines in the UI layer.
*   **Idea #240**: Centralize `ContextShadow` instantiation in a Dagger/Hilt module. Currently, it's manually instantiated as a local property in each core service/provider. Providing a `@ShadowContext` qualified `Context` via Hilt would ensure consistent singleton application and simplify class constructors.
