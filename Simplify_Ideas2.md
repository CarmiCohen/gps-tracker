# Strategic Simplification Ideas (Batch 2)

## 🎯 Architecture & Data Layer
*   **Idea #1: DataStore List Mutation Extension**: Extract the atomic `addHomePoint`/`removeHomePoint` pattern into a generic extension function for `DataStore<AppSettings>`. This would allow race-free updates for any repeated field in the proto (e.g., future alert categories or device groups) without duplicating `updateData` logic. (Origin: Sep.22.03, Issue #1179).
*   **Idea #2: UseCase Functional Consolidation**: Consolidate `HomePointUseCase` and `MapUseCase` into a single `SpatialLogicUseCase` to further reduce ViewModel dependency injection surface area.
