# Strategic Simplification Ideas (Batch 2)

## 🎯 Architecture & Data Layer
*   **Idea #1: DataStore List Mutation Extension** (Integrated in Sep.22.04)
    *   *Status*: Resolved under Issue #1180.
*   **Idea #2: UseCase Functional Consolidation**: Consolidate `HomePointUseCase` and `MapUseCase` into a single `SpatialLogicUseCase` to further reduce ViewModel dependency injection surface area.
