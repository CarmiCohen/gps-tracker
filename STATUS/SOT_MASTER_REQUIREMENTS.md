# SOT Master Requirements & Hardening Status (Oct14.1)

## 🏗️ Architectural Master Rules (265 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.207 Pure Mapping Architecture (R-ID 725)**: UI state mapping logic MUST be extracted from `MainViewModel` into dedicated, pure mapper objects (e.g., `HudMapper`, `DashboardMapper`, `MapStateMapper`) to ensure ViewModel maintainability, testability, and separation of concerns. (Oct14.1 - SIMP-OCT14.21-S1).
*   **1.208 Lock-Contention-Free IO (R-ID 726)**: High-frequency IO operations on memory-mapped buffers (e.g., `MappedByteBuffer.force()`) MUST be executed outside of critical synchronized blocks to prevent UI stalls and thread contention. (Oct14.1 - SIMP-OCT14.11-S1).
*   **1.209 Primitive-Backed Telemetry Buffering (R-ID 727)**: High-volume telemetry data (e.g., trail points) MUST be stored in primitive-backed structures (e.g., `TrailBuffer` with `DoubleArray`) instead of lists of objects to minimize GC pressure and heap fragmentation during long-duration tracking. (Oct14.1 - SIMP-OCT15.0-S1).
*   **1.210 Primitive-Backed History Buffering (R-ID 728)**: High-frequency forensic history data (e.g., connection ribbons) MUST be emitted and consumed as primitive-backed structures (e.g., `HistoryBuffer`) to eliminate O(N) object allocation overhead in UI update cycles. (Oct14.1 - SIMP-OCT15.1-S1).
*   **1.211 Primitive Map Rendering Segments (R-ID 729)**: Intermediate map rendering structures (e.g., `MapTrailSegment`) MUST use primitive arrays for coordinate storage to avoid `GeoPoint` object creation in the mapping pipeline. (Oct14.1 - SIMP-OCT15.1-S2).
*   **1.212 Offloaded Telemetry Normalization (R-ID 730)**: Telemetry data normalization (e.g., scaling 0.0-1.0 for UI ribbons) MUST be performed within background data pipelines (e.g., `MainRepository` mapping) and stored in primitive arrays within `HistoryBuffer` to eliminate computational overhead on the UI thread. (Oct14.1 - SIMP-OCT15.2-S1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 735**: Ribbon Selector Consolidation - PASSED. Pre-calculated normalized telemetry within `HistoryBuffer` primitive arrays, offloading scaling logic from the UI thread. (Oct14.1 - SIMP-OCT15.2-S1).
*   **SOT ID 734**: Map Primitive Integration - PASSED. Transitioned `MapTrailSegment` to use primitive arrays, eliminating `GeoPoint` churn in `MapStateMapper`. (Oct14.1 - SIMP-OCT15.1-S2).
*   **SOT ID 733**: History Buffer Optimization - PASSED. Refactored forensic history flows to use `HistoryBuffer`, reducing object allocations per emission from O(N) to O(1). (Oct14.1 - SIMP-OCT15.1-S1).
*   **SOT ID 732**: Trail Buffer Optimization - PASSED. Implemented `TrailBuffer` using primitive arrays for all trail flows, reducing object overhead from O(N) to O(1) per flow emission. (Oct14.1 - SIMP-OCT15.0-S1).
*   **SOT ID 731**: Map Architectural Extraction - PASSED. Extracted MapViewState and Trail Segment mapping logic into MapStateMapper, completing the pure mapping transition for all major UI slices. (Oct14.1 - SIMP-OCT14.21-S1).

...

---

## ✅ Verification Chapters
*   **Chapter 31.810 (Offloaded Normalization)**: PASSED - Verified zero runtime scaling overhead on UI thread during ribbon rendering via profiling. (Oct14.1).
*   **Chapter 31.809 (Map Primitive Integration)**: PASSED - Verified zero `GeoPoint` allocation during trail mapping via memory profiler. (Oct14.1).
*   **Chapter 31.808 (History Buffer Optimization)**: PASSED - Verified GC pressure reduction during ribbon rendering on budget hardware. (Oct14.1).
*   **Chapter 31.807 (Trail Buffer Optimization)**: PASSED - Verified object overhead reduction via primitive-backed TrailBuffer flows. (Oct14.1).
