# SOT Master Requirements & Hardening Status (Oct14.1)

## 🏗️ Architectural Master Rules (272 Rules)

### 1. Lifecycle & Resource Management
*   **1.207 Pure Mapping Architecture (R-ID 725)**: UI state mapping logic MUST be extracted from `MainViewModel` into dedicated, pure mapper objects (e.g., `HudMapper`, `DashboardMapper`, `MapStateMapper`) to ensure ViewModel maintainability, testability, and separation of concerns. (Oct14.1 - SIMP-OCT14.21-S1).
*   **1.208 Lock-Contention-Free IO (R-ID 726)**: High-frequency IO operations on memory-mapped buffers (e.g., `MappedByteBuffer.force()`) MUST be executed outside of critical synchronized blocks to prevent UI stalls and thread contention. (Oct14.1 - SIMP-OCT14.11-S1).
*   **1.209 Primitive-Backed Telemetry Buffering (R-ID 727)**: High-volume telemetry data (e.g., trail points) MUST be stored in primitive-backed structures (e.g., `TrailBuffer` with `DoubleArray`) instead of lists of objects to minimize GC pressure and heap fragmentation during long-duration tracking. (Oct14.1 - SIMP-OCT15.0-S1).
*   **1.210 Primitive-Backed History Buffering (R-ID 728)**: High-frequency forensic history data (e.g., connection ribbons) MUST be emitted and consumed as primitive-backed structures (e.g., `HistoryBuffer`) to eliminate O(N) object allocation overhead in UI update cycles. (Oct14.1 - SIMP-OCT15.1-S1).
*   **1.211 Primitive Map Rendering Segments (R-ID 729)**: Intermediate map rendering structures (e.g., `MapTrailSegment`) MUST use primitive arrays for coordinate storage to avoid `GeoPoint` object creation in the mapping pipeline. (Oct14.1 - SIMP-OCT15.1-S2).
*   **1.212 Offloaded Telemetry Normalization (R-ID 730)**: Telemetry data normalization (e.g., scaling 0.0-1.0 for UI ribbons) MUST be performed within background data pipelines (e.g., `MainRepository` mapping) and stored in primitive arrays within `HistoryBuffer` to eliminate computational overhead on the UI thread. (Oct14.1 - SIMP-OCT15.2-S1).
*   **1.213 Centralized Telemetry Mapping (R-ID 731)**: All telemetry-to-buffer and entity-to-buffer transformations MUST be centralized within `TelemetryMapper` to ensure normalization consistency and eliminate redundancy across repositories and use cases. (Oct14.1 - SIMP-OCT15.4-S1).
*   **1.214 Encapsulated Buffer Construction (R-ID 732)**: Instantiation, normalization, and reconciliation (merging) logic for complex primitive-backed buffers MUST be encapsulated within the buffer's companion object (e.g., `HistoryBuffer.Companion.fromPoints`) to reduce call-site complexity and leakage of imperative array-handling logic. (Oct14.1 - SIMP-OCT15.4-S2).
*   **1.215 Explicit Persistence Gating (R-ID 733)**: All IO operations (Trails, Violations, History, Maintenance) MUST be gated by `PersistencePolicy` to ensure system stability during critical health states like Mali anomalies or low battery. (Oct14.1 - ISSUE-OCT15.7-F1).
*   **1.216 Unified Log Gating (R-ID 734)**: Application logging MUST be gated by `PersistencePolicy.shouldSaveLog()` to prevent driver-level I/O contention during Mali anomalies and ensure consistent storage pressure handling across all diagnostic paths. (Oct14.1 - ISSUE-OCT15.9-F1).
*   **1.217 Defensive Telemetry Clamping (R-ID 735)**: High-frequency diagnostic fields (especially timestamps, floors, and debounce values) MUST be clamped at both the hardware ingestion point and the mapping boundary to prevent JNI buffer corruption or peer-to-peer garbage from causing UI overflows or runaway jitter. (Oct14.1 - ISSUE-OCT15.11-F1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 742**: Vibration Floor Hardening - PASSED. Implemented [0.02, 2.0] G boundary clamping in jdhardware-jni.cpp (n13, n19), SentinelValidator JVM fallback, and TelemetryMapper ingestion paths. (Oct14.1 - ISSUE-OCT15.11-F1).
*   **SOT ID 741**: Proximity Debounce Hardening - PASSED. Corrected typo in HardwareSuite, added JNI result validation, and enforced boundary clamping in TelemetryMapper. (Oct14.1 - ISSUE-OCT15.10-F2).
*   **SOT ID 740**: Unit Test Restoration - PASSED. Decoupled test suite from MainViewModel and aligned with Pure Mapping Architecture, restoring 100% pass rate. (Oct14.1 - ISSUE-OCT15.10-F1).
*   **SOT ID 739**: Log Gating Integration - PASSED. Refactored LogManager to use centralized PersistencePolicy, ensuring Mali driver protection and consistent storage pressure handling. (Oct14.1 - ISSUE-OCT15.9-F1).
*   **SOT ID 738**: Persistence Policy Audit - PASSED. Expanded PersistencePolicy to cover all IO paths and enforced gating in MainRepository. (Oct14.1 - ISSUE-OCT15.7-F1).

...

---

## ✅ Verification Chapters
*   **Chapter 31.817 (Vibration Floor Stability)**: PASSED - Confirmed that `adaptiveVibrationFloor` is clamped within [0.02, 2.0] G range in both native fast-path and Kotlin mapping layers, preventing the "runaway" jitter reported on A15 diagnostics. (Oct14.1).
*   **Chapter 31.816 (Diagnostic Data Integrity)**: PASSED - Verified that `proximityDebounceMs` remains within 0..15000L range even when uninitialized JNI buffers or corrupted JSON payloads are processed. (Oct14.1).
*   **Chapter 31.815 (Unit Test Integrity)**: PASSED - Confirmed 100% unit test pass rate (88 tests) across :app and :core:engine modules following architectural decoupling. (Oct14.1).
*   **Chapter 31.814 (Unified Log Gating)**: PASSED - Confirmed via `StoragePressureAuditTest` that Normal and Important logs are correctly inhibited during simulated Low/Critical storage pressure, while Special logs persist. (Oct14.1).
