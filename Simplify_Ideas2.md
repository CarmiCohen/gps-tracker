# Simplification Ideas (Aug.18.13)

Ideas to simplify the codebase and improve maintainability:

1. **Unify Buffer Management**: The `ForensicSpillBuffer` and `LogRepository` drain logic are highly optimized but complex. Consider a higher-level abstraction for "Durable Streams" that hides the MappedByteBuffer complexity from the repository layer.
2. **Atomic Counter Consolidation**: Currently, multiple `AtomicInteger` objects are used in `MainRepository`. These could be grouped into a single `PerformanceMetrics` data class/structure to simplify the repository's state management.
3. **Compose UI Decoupling**: While `derivedStateOf` has helped, further decoupling the map overlay logic from the main UI thread using a dedicated `MapStateProducer` could reduce the cognitive load of the `AppMapContainer`.
4. **Remove Legacy Regex Logic**: If forensic signatures now guarantee uniqueness and integrity via CRC32, evaluate if the legacy regex-based deduplication in the persistence layer can be entirely removed to simplify `LogRepository`.
