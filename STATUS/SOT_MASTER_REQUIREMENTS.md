# SOT Master Requirements & Hardening Status (Oct14.1)

## 🏗️ Architectural Master Rules (268 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.207 Pure Mapping Architecture (R-ID 725)**: UI state mapping logic MUST be extracted from `MainViewModel` into dedicated, pure mapper objects (e.g., `HudMapper`, `DashboardMapper`, `MapStateMapper`) to ensure ViewModel maintainability, testability, and separation of concerns. (Oct14.1 - SIMP-OCT14.21-S1).
*   **1.208 Lock-Contention-Free IO (R-ID 726)**: High-frequency IO operations on memory-mapped buffers (e.g., `MappedByteBuffer.force()`) MUST be executed outside of critical synchronized blocks to prevent UI stalls and thread contention. (Oct14.1 - SIMP-OCT14.11-S1).
*   **1.209 Primitive-Backed Telemetry Buffering (R-ID 727)**: High-volume telemetry data (e.g., trail points) MUST be stored in primitive-backed structures (e.g., `TrailBuffer` with `DoubleArray`) instead of lists of objects to minimize GC pressure and heap fragmentation during long-duration tracking. (Oct14.1 - SIMP-OCT15.0-S1).
*   **1.210 Primitive-Backed History Buffering (R-ID 728)**: High-frequency forensic history data (e.g., connection ribbons) MUST be emitted and consumed as primitive-backed structures (e.g., `HistoryBuffer`) to eliminate O(N) object allocation overhead in UI update cycles. (Oct14.1 - SIMP-OCT15.1-S1).
*   **1.211 Primitive Map Rendering Segments (R-ID 729)**: Intermediate map rendering structures (e.g., `MapTrailSegment`) MUST use primitive arrays for coordinate storage to avoid `GeoPoint` object creation in the mapping pipeline. (Oct14.1 - SIMP-OCT15.1-S2).
*   **1.212 Offloaded Telemetry Normalization (R-ID 730)**: Telemetry data normalization (e.g., scaling 0.0-1.0 for UI ribbons) MUST be performed within background data pipelines (e.g., `MainRepository` mapping) and stored in primitive arrays within `HistoryBuffer` to eliminate computational overhead on the UI thread. (Oct14.1 - SIMP-OCT15.2-S1).
*   **1.213 Centralized Telemetry Mapping (R-ID 731)**: All telemetry-to-buffer and entity-to-buffer transformations MUST be centralized within `TelemetryMapper` to ensure normalization consistency and eliminate redundancy across repositories and use cases. (Oct14.1 - SIMP-OCT15.4-S1).
*   **1.214 Encapsulated Buffer Construction (R-ID 732)**: Instantiation, normalization, and reconciliation (merging) logic for complex primitive-backed buffers MUST be encapsulated within the buffer's companion object (e.g., `HistoryBuffer.Companion.fromPoints`) to reduce call-site complexity and leakage of imperative array-handling logic. (Oct14.1 - SIMP-OCT15.4-S2).
*   **1.215 Explicit Persistence Gating (R-ID 733)**: All IO operations (Trails, Violations, History, Maintenance) MUST be gated by `PersistencePolicy` to ensure system stability during critical health states like Mali anomalies or low battery. (Oct14.1 - ISSUE-OCT15.7-F1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 738**: Persistence Policy Audit - PASSED. Expanded PersistencePolicy to cover all IO paths and enforced gating in MainRepository. (Oct14.1 - ISSUE-OCT15.7-F1).
*   **SOT ID 737**: Buffer Constructor Refactoring - PASSED. Encapsulated HistoryBuffer array initialization and merging into its companion object, simplifying UseCase and Mapper layers. (Oct14.1 - SIMP-OCT15.4-S2).
*   **SOT ID 736**: Mapper Consolidation - PASSED. Centralized HistoryBuffer mapping in `TelemetryMapper`, eliminating duplicate logic in `MainRepository` and `StateSubscriptionUseCase`. (Oct14.1 - SIMP-OCT15.4-S1).

...

---

## ✅ Verification Chapters
*   **Chapter 31.813 (Explicit Persistence Gating)**: PASSED - Verified that Violations and Pruning are strictly inhibited during simulated Mali anomalies via Logcat audit. (Oct14.1).
*   **Chapter 31.812 (Encapsulated Construction)**: PASSED - Verified HistoryBuffer instantiation and merging logic is fully contained within the model, with simplified call sites in UseCase and Mapper. (Oct14.1).
