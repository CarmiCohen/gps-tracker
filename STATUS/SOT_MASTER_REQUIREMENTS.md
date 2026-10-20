# SOT Master Requirements & Hardening Status (Oct20.0)

## 🏗️ Architectural Master Rules (300 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules 1.1 - 1.206 archived in sot_master_archive.md)
*   **1.226 Document Integrity Baseline (R-ID 744)**: To protect against accidental truncation or corruption of critical status documents, the system MUST maintain a versioned baseline directory (`STATUS/BASELINE/[Version]/`). At the end of each version cycle, the current `SOT_MASTER_REQUIREMENTS.md` and `RESOLUTION_ARCHIVE.md` MUST be copied to this directory as immutable references. (Oct20.0 - ISSUE-OCT20.0-F1).
*   **1.225 Test Mode Restoration Bypass (R-ID 743)**: To allow deterministic testing of mode transitions and prevent "boot loops" into Tracker mode, the system MUST provide a persistent bypass for automatic service resumption (R-ID 242). This bypass MUST be toggleable via Diagnostics and respected by the MainViewModel during cold-start initialization. (Oct19.19 - ISSUE-OCT19.19-F1).
*   **1.224 Unified Network Callback Management (R-ID 742)**: To prevent redundant system overhead and log flooding, the system MUST maintain exactly one `ConnectivityManager.NetworkCallback` registration. This authority MUST reside in `NetworkProvider`. All other system status observers MUST consume the unified reactive stream provided by this provider. (Oct18.24 - ISSUE-OCT18.22-F2).
*   **1.223 Battery Saver Signaling Policy (R-ID 741)**: The system MUST explicitly account for system-wide Battery Saver (Power Save) mode in its signaling logic. Non-critical telemetry sync MUST be deferred when Battery Saver is active to preserve power, while ensuring immediate resumption if a security violation occurs. Forensic logs MUST reflect the unified "(Doze/PowerSave active)" state for auditability. (Oct18.24 - ISSUE-OCT18.22-F1).
*   **1.222 Test Isolation Lifecycle (R-ID 740)**: To ensure deterministic behavior in multi-threaded test environments, repositories MUST provide hooks to manually trigger or inhibit background batch processors and drainers. (Oct18.18 - ISSUE-OCT18.18-F1).
*   **1.221 Doze Mode White-listing (R-ID 739)**: The system MUST allow telemetry signaling during Android Doze mode (Device Idle) if the application is explicitly white-listed by the user ("Ignoring Battery Optimizations"). This ensures high-persistence forensic continuity even when the OS attempts to defer network activity. (Oct18.16 - ISSUE-OCT18.16-F1).
*   **1.220 Urban Reflectivity Altitude Gating (R-ID 738)**: To harden stationary anchors against urban multipath, the system MUST apply extreme skepticism (0.25x breakout score multiplier) when a high-SNR reflection (SNR ≥ 35) is accompanied by a significant altitude shift (≥ 3.5m). (Oct18.14 - ISSUE-OCT18.14-F1).
*   **1.219 Acoustic Duty Cycle Overlap (R-ID 737)**: To ensure gapless capture of acoustic events during power-save duty cycles, the microphone MUST start recording at least 500ms (OVERLAP_MS) before the OFF cycle ends. (Oct18.12 - ISSUE-OCT18.12-F1).
*   **1.218 Steep-Drop Vibration Convergence (R-ID 736)**: The adaptive vibration floor MUST implement accelerated downward convergence (Steep-Drop) when the current vibration energy drops below 40% of the existing floor. (Oct18.10 - ISSUE-OCT18.10-F1).
*   **1.217 SNR Latch for Urban Multipath (R-ID 735)**: The system MUST implement a sustained security hold (SNR Latch) for location jumps that present with high SNR (SNR ≥ 35). (Oct18.8 - ISSUE-OCT18.8-F1).
*   **1.216 Synthetic Physical Validation (R-ID 734)**: The system MUST provide synthetic injection hooks for all physical hardware dependencies. (Oct18.6 - ISSUE-OCT18.6-F1).
*   **1.215 Explicit Component Lifecycle (R-ID 733)**: Core repositories and managers MUST NOT initiate background processing or side-effect heavy operations within their constructors. Lifecycle control MUST be delegated to the Application or Service layer via explicit `start()` methods. (Oct18.4 - SIMP-OCT18.2-S1).
*   **1.214 Encapsulated Buffer Construction (R-ID 732)**: Instantiation, normalization, and reconciliation logic for complex primitive-backed buffers MUST be encapsulated within the buffer's companion object (e.g., `HistoryBuffer.Companion.fromPoints`) to reduce call-site complexity. (Oct14.1 - SIMP-OCT15.4-S2).
*   **1.213 Centralized Telemetry Mapping (R-ID 731)**: All telemetry-to-buffer and entity-to-buffer transformations MUST be centralized within `TelemetryMapper` to ensure normalization consistency and eliminate redundancy across repositories. (Oct14.1 - SIMP-OCT15.4-S1).
*   **1.212 Offloaded Telemetry Normalization (R-ID 730)**: Telemetry data normalization (e.g., scaling 0.0-1.0 for UI ribbons) MUST be performed within background data pipelines and stored in primitive arrays within `HistoryBuffer` to eliminate computational overhead on the UI thread. (Oct14.1 - SIMP-OCT15.2-S1).
*   **1.211 Primitive Map Rendering Segments (R-ID 729)**: Intermediate map rendering structures MUST use primitive arrays for coordinate storage to avoid `GeoPoint` object creation in the mapping pipeline. (Oct14.1 - SIMP-OCT15.1-S2).
*   **1.210 Primitive-Backed History Buffering (R-ID 728)**: High-frequency forensic history data (e.g., connection ribbons) MUST be emitted and consumed as primitive-backed structures (e.g., `HistoryBuffer`) to eliminate O(N) object allocation overhead. (Oct14.1 - SIMP-OCT15.1-S1).
*   **1.209 Primitive-Backed Telemetry Buffering (R-ID 727)**: High-volume telemetry data MUST be stored in primitive-backed structures (e.g., `TrailBuffer` with `DoubleArray`) instead of lists of objects to minimize GC pressure and heap fragmentation. (Oct14.1 - SIMP-OCT15.0-S1).
*   **1.208 Lock-Contention-Free IO (R-ID 726)**: High-frequency IO operations on memory-mapped buffers (e.g., `MappedByteBuffer.force()`) MUST be executed outside of critical synchronized blocks to prevent UI stalls and thread contention. (Oct14.1 - SIMP-OCT14.11-S1).
*   **1.207 Pure Mapping Architecture (R-ID 725)**: UI state mapping logic MUST be extracted from `MainViewModel` into dedicated, pure mapper objects (e.g., `HudMapper`, `DashboardMapper`, `MapStateMapper`) to ensure ViewModel maintainability. (Oct14.1 - SIMP-OCT14.21-S1).

... [Remaining historical rules archived in STATUS/sot_master_archive.md] ...

## 🛡️ Core Hardening Baseline
*   **SOT ID 776**: Completion Sequence Hardening - PASSED. Updated DEVELOPER_GUIDELINES.md to enforce Forensic Integrity Checks (Rule 8) and Baseline Synchronization. (Oct20.0 - ISSUE-OCT19.20-F2).
*   **SOT ID 775**: Document Integrity Baseline - PASSED. Established versioned baseline directory and recorded R-ID 744 to protect against future document truncation. (Oct20.0 - ISSUE-OCT20.0-F1).
*   **SOT ID 774**: SOT Master Requirements Recovery - PASSED. Fully restored missing rules and baseline entries from historical stable commits after accidental shortening. (Oct19.20 - ISSUE-OCT19.20-F1).
*   **SOT ID 773**: Test Mode Restoration Bypass - PASSED. Implemented persistent `isRestorationDisabled` flag and ViewModel hook to bypass R-ID 242. (Oct19.19 - ISSUE-OCT19.19-F1).
*   **SOT ID 772**: Unified Network Callback Management - PASSED. Consolidated all `ConnectivityManager` registrations into `AndroidNetworkProvider` and refactored `SystemStatusProviderImpl` to consume the unified flow. (Oct18.24 - ISSUE-OCT18.22-F2).
*   **SOT ID 771**: Battery Saver Signaling Policy - PASSED. Updated `ConnectivitySuite` logging to explicitly reflect `PowerSave` status in forensic traces, ensuring compliance with Chapter 38 audit signatures. (Oct18.24 - ISSUE-OCT18.22-F1).
*   **SOT ID 770**: Test Isolation Lifecycle - PASSED. Added `triggerManualBatchProcessor` and improved test synchronization in `StoragePressureAuditTest`. (Oct18.18 - ISSUE-OCT18.18-F1).
*   **SOT ID 769**: Doze Mode White-listing - PASSED. Implemented `isIgnoringBatteryOptimizations` check in `PowerStateProvider` and updated `HardwareSuite` to allow signaling bypass for white-listed sessions. (Oct18.16 - ISSUE-OCT18.16-F1).
*   **SOT ID 768**: Urban Signal Reflectivity Hardening - PASSED. Implemented 3.5m altitude gate skepticism in `AnchorEvaluator` for high-SNR reflections. (Oct18.14 - ISSUE-OCT18.14-F1).
*   **SOT ID 767**: Acoustic Duty Cycle Overlap - PASSED. Implemented 500ms transition buffering in `HardwareSuite.acousticThread`. (Oct18.12 - ISSUE-OCT18.12-F1).
*   **SOT ID 766**: Adaptive Vibration Floor Convergence - PASSED. Implemented Steep-Drop convergence logic. (Oct18.10 - ISSUE-OCT18.10-F1).
*   **SOT ID 765**: Urban Multipath Hardening - PASSED. Implemented 360s SNR Latch and high-SNR anchor skepticism. (Oct18.8 - ISSUE-OCT18.8-F1).
*   **SOT ID 764**: Synthetic Physical Injection - PASSED. Implemented `SyntheticEventInjector`. (Oct18.6 - ISSUE-OCT18.6-F1).
*   **SOT ID 763**: Explicit Repository Lifecycle - PASSED. Refactored `LogRepository` and `LogManager` to use explicit `start()` triggers. (Oct18.4 - SIMP-OCT18.2-S1).
*   **SOT ID 762**: Test Mode DI Refactoring - PASSED. Migrated `LogRepository` to `TestModeProvider` injection, eliminating static leakage. (Oct18.2 - SIMP-OCT17.11-S1).
*   **SOT ID 761**: JNI GNSS Alignment Hardening - PASSED. Re-aligned `GnssHealthBatch` to 8-byte boundaries to resolve A15 `NaNG` regression. (Oct18.0 - ISSUE-OCT16.9-F2).
*   **SOT ID 760**: System Audit & Procedure Synchronization - PASSED. Synchronized `TEST_PROCEDURE.md` to 100 chapters and verified all logic. (Oct17.11 - TEST-OCT17.11-T2).
*   **SOT ID 759**: Instrumentation Test Isolation - PASSED. Inhibited background drainer during wrap-around stability tests. (Oct17.11 - TEST-OCT17.11-T1).
*   **SOT ID 758**: Build Dependency Hardening - PASSED. Corrected Compose BOM platform alignment for AndroidTest. (Oct17.11 - TEST-OCT17.11-T1).
*   **SOT ID 757**: Automated Version Synchronization - PASSED. Centralized versioning in `gradle.properties` and injected via `BuildConfig`. (Oct17.10 - SIMP-OCT15.6-S1).
*   **SOT ID 756**: Version Name Verification - PASSED. Implemented formal version verification step in Chapter 1.3. (Oct15.6 - ISSUE-OCT15.6-T1).
*   **SOT ID 755**: Project-Wide Test Cycle - PASSED. Verified 100% pass rate for automated test suite. (Oct18.8).
*   **SOT ID 754**: JNI Memory Pressure Parity - PASSED. Hardened JNI pipeline for consistent memory pressure throttling across all sensor paths. (Oct18.8 - SOT ID 754).
*   **SOT ID 753**: JNI Telemetry Autonomy Hardening - PASSED. Migrated telemetry evaluation logic to C++ to ensure consistency. (Oct18.8 - SOT ID 753).
*   **SOT ID 752**: Vibration NaN Hardening & JNI Alignment - PASSED. Resolved NaNG regression on Samsung A15. (Oct18.8 - SOT ID 752).
*   **SOT ID 751**: Native Clamp Unification - PASSED. Unified vibration and proximity limits via EngineConstants.h. (Oct14.1 - SIMP-OCT15.12-S1).
*   **SOT ID 750**: Unit Test Restoration - PASSED. Decoupled test suite from MainViewModel. (Oct14.1 - ISSUE-OCT15.10-F1).
*   **SOT ID 749**: Log Gating Integration - PASSED. Refactored LogManager to use centralized PersistencePolicy. (Oct14.1 - ISSUE-OCT15.9-F1).
*   **SOT ID 748**: Persistence Policy Audit - PASSED. Expanded PersistencePolicy to cover all IO paths. (Oct14.1 - ISSUE-OCT15.7-F1).
*   **SOT ID 747**: Test Utility Decoupling - PASSED. Refactored MainFileHelper. (Oct14.1 - SIMP-OCT15.10-S1).
*   **SOT ID 746**: Hydration Stability Audit - PASSED. Resolved timeout stalls by pre-warming OSM state. (Oct14.1 - ISSUE-OCT16.2-F1).
*   **SOT ID 745**: Test State Pre-warming Audit - PASSED. Formalized global state reset in GpsApplication. (Oct14.1 - SIMP-OCT16.3-S1).
*   **SOT ID 744**: Immutable Ribbon Normals - PASSED. Moved normalization to TelemetryMapper and introduced RibbonNormals container. (Oct14.1 - SIMP-OCT15.21-S1).
*   **SOT ID 743**: Animation Controller Composition - PASSED. Centralized Mali gating via LocalAnimationGating. (Oct14.1 - SIMP-OCT15.20-S1).
*   **SOT ID 742**: Vibration Floor Hardening - PASSED. Implemented G boundary clamping in native and JVM layers. (Oct14.1 - ISSUE-OCT15.11-F1).
*   **SOT ID 741**: Proximity Debounce Hardening - PASSED. Corrected typo and added JNI result validation. (Oct14.1 - ISSUE-OCT15.10-F2).
*   **SOT ID 740**: Synthetic Physical Injection - PASSED. Implemented SyntheticEventInjector. (Oct18.6 - ISSUE-OCT18.6-F1).
*   **SOT ID 739**: SNR Latch for Urban Multipath - PASSED. Implemented 360s security hold. (Oct18.8 - ISSUE-OCT18.8-F1).
*   **SOT ID 738**: Urban Reflectivity Altitude Gating - PASSED. Implemented 3.5m altitude gate. (Oct18.14 - ISSUE-OCT18.14-F1).

... [Remaining baseline entries preserved in historical archives] ...

---

## ✅ Verification Chapters
*   **Chapter 31.820 (Ribbon Normalization Audit)**: PASSED - Verified that 18/18 analytical ribbons consume pre-calculated normals. (Oct14.1).
*   **Chapter 31.819 (Animation Composition Audit)**: PASSED - Verified that all HUD components consume Mali state via `LocalAnimationGating`. (Oct14.1).
*   **Chapter 1-100 (Full Hardening Cycle)**: PASSED - Verified counter stability and state persistence over 24h soak simulation. (Oct18.8).
