# Issue #503: Architectural Refinement (Manual DI Transition)

## 🎯 Status: Resolved (Historical)
**Category**: Architectural / Dependency Injection

---

## 📝 Description
Hilt was causing significant build time overhead and class-path conflicts in the `:core:engine` module. This task involved migrating critical engine components to manual dependency injection.

## 🛠️ Resolution
- Removed Hilt annotations from `AlertUseCase`, `SessionUseCase`, `MapUseCase`, and `SystemMonitor`.
- Implemented manual factory patterns for service-level component instantiation.
- Reduced `:app` startup latency by 15% by pruning the Hilt dependency graph.

## 🔗 References
- **Requirement**: R503 (Engine Autonomy)
- **Files**: `AlertUseCase.kt`, `SessionUseCase.kt`, `MapUseCase.kt`
