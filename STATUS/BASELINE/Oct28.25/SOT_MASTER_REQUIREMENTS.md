# SOT Master Requirements & Hardening Status (Oct28.25)

## 🏗️ Architectural Master Rules (369 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Service Residency (R101)**: The Tracker background service MUST maintain a foreground priority with a visible notification to prevent OS-level reclamation. (v8.8.0).
*   **1.2 Job Persistence (R102)**: Critical telemetry sync jobs MUST utilize `WorkManager` with `EXPEDITED` flags to bridge service restarts and process death. (v8.8.0).
*   **1.3 Monotonic Clock Authority (R103)**: All forensic timestamps MUST be calculated using `SystemClock.elapsedRealtime()` to prevent drift from user-initiated NTP changes. (v8.8.0).
*   **1.4 Proactive Log Pruning (R104)**: The system MUST execute a `deepPruneLogs` operation during startup if the database size exceeds 250MB to prevent I/O stalls on budget hardware. (v8.8.0 / Implemented Oct27.10 - ISSUE-OCT26.2-F1).
*   **1.273 Signaling Transparency (R-ID 512)**: The DAT badge MUST provide real-time numerical feedback for conflated packets through a numerical overlay and long-press diagnostic toast to ensure forensic visibility into pipeline pressure. (Oct27.10 - ISSUE-OCT27.0-F1).
*   **1.274 Forensic Persistence Hardening (R-ID 744)**: The system MUST enforce a UNIQUE constraint on `LogEntity.localId` and utilize optimized drain batching (2s interval, 2000 batch cap) to sustain 100Hz telemetry bursts without I/O wait (IOW) degradation. (Oct27.10 - ISSUE-OCT27.1-F1).
*   **1.275 Test Worker Factory (R-ID 745)**: The system MUST utilize a shared `BaseGpsApplication` that provides `HiltWorkerFactory` to `WorkManager` via an EntryPoint to ensure consistent background job initialization across production and instrumented test environments, preventing `NoSuchMethodException`. (Oct27.10 - SIMP-OCT27.4-S1).
*   **1.276 Signaling Audit Synchronization (R-ID 746)**: Production readiness audits MUST verify signaling connectivity via `TestModeProvider` before burst injection to ensure deterministic conflation traces in high-pressure simulation. (Oct27.10 - ISSUE-OCT27.4-F2).
*   **1.277 HUD Component Scaling (R-ID 747)**: HUD status indicators MUST utilize a shared `MeasurableBadge` component to enforce consistent layout metrics and reduce redundant Compose overhead. (Oct27.10 - ISSUE-OCT27.5-F1).
*   **1.278 Forensic Recovery Integrity (R-ID 748)**: The system MUST reliably recover forensic telemetry from the off-heap `ForensicSpillBuffer` upon process restart and persist it to the primary database before advancing the buffer's read pointer to ensure zero-loss integrity. (Oct27.10 - ISSUE-OCT27.6-F1).
*   **1.279 Staggered Persistence (R-ID 749)**: The forensic spill-buffer MUST implement staggered `force()` calls (5s interval) during non-emergency states to mitigate SPI bus contention and prevent telemetry jitter on budget hardware (A15). Full synchronization MUST be enforced during recovery and high-pressure (90%) fill states. (Oct27.10 - ISSUE-OCT27.7-F1).
*   **1.280 Database Recovery Lock (R-ID 750)**: The system MUST utilize an exclusive Mutex (`recoveryLock`) during the forensic recovery phase to block standard log drainers and maintenance workers, preventing database contention and ensuring recovery trace replay integrity. (Oct27.10 - ISSUE-OCT27.8-F1).
*   **1.281 IO Latency-Driven Throttle (R-ID 751)**: The forensic drainer MUST dynamically scale its drain interval and batch size based on real-time `maxIoLatency` feedback (threshold: 500ms) to prevent storage-induced telemetry jitter on budget hardware during sustained load. (Oct27.10 - ISSUE-OCT27.9-F1).
*   **1.282 Latency EMA Smoothing (R-ID 752)**: The forensic drainer MUST utilize an Exponential Moving Average (EMA) for `maxIoLatency` feedback (Alpha: 0.2) to smooth out throttling response and prevent rapid oscillation during transient storage micro-spikes. (Oct27.10 - ISSUE-OCT27.10-S1).
*   **1.283 Pruning Batch Optimization (R-ID 753)**: Maintenance tasks MUST dynamically scale the pruning chunk size (500-5000) based on the backlog delta relative to retention limits. This ensures that deep prunes during startup or high-pressure states recover storage space rapidly without inducing sustained I/O stalls. (Oct27.10 - SIMP-OCT27.10-S1).
*   **1.284 Pruning Worker Telemetry (R-ID 754)**: The system MUST report the total purged record count per maintenance cycle to telemetry to monitor the impact of dynamic batching and ensure visibility into storage maintenance efficiency. (Oct27.10 - SIMP-OCT28.0-S1).
*   **1.285 Database Transaction Timeout (R-ID 755)**: Critical database operations gated by the `recoveryLock` MUST be protected by a `withTimeout(DATABASE_TRANSACTION_TIMEOUT_MS)` watchdog (30s) to prevent permanent system stalls in the event of a hung SQLite transaction or I/O deadlock. (Oct27.10 - SIMP-OCT27.8-S1).
*   **1.286 Pruning Telemetry Aggregation (R-ID 756)**: The system MUST calculate and report an Exponential Moving Average (EMA) of purged record counts (Alpha: 0.3) to provide a smoothed visualization of maintenance efficiency over time in forensic telemetry. (Oct27.10 - SIMP-OCT28.2-S1).
*   **1.287 Recovery Buffer Scaling (R-ID 757)**: The `ForensicSpillBuffer` MUST dynamically scale its capacity (2,000 to 50,000 entries) based on available storage health to prevent over-provisioning and optimize resource usage on constrained devices while maximizing retention on high-capacity hardware. (Oct27.10 - SIMP-OCT27.6-S1).
*   **1.288 Settings Integrity Guard (R-ID 758)**: The `SettingsRepository` MUST implement a hash-based integrity check (SHA-256) for its persistent state to detect data corruption that may occur during OS-level backup/restore events or unexpected process deaths. (Oct27.10 - SIMP-OCT28.11-S1).
*   **1.289 Settings Restoration Policy (R-ID 759)**: The `SettingsRepository` MUST implement an emergency factory reset fallback that triggers after a threshold of consecutive integrity signature failures (3) is reached across process restarts. This ensures the application can recover a functional state from persistent corruption. (Oct27.10 - SIMP-OCT28.12-S1).
*   **1.290 Badge Measurement Caching (R-ID 760)**: `MeasurableBadge` MUST utilize a custom `Layout` block and flattened hierarchy to minimize measurement passes and bypass high-level `Alignment` overhead during rapid-fire HUD status updates. (Oct27.10 - SIMP-OCT27.5-S1).
*   **1.291 Stealth Suppression Authority (R-ID 761)**: The system MUST enforce role-based suppression of all audible sirens and visual alarm overlays on devices in Tracker mode to ensure operational stealth (R872). Suppression MUST be enforced at the engine level (AudioSynthesizer) and UI service level (AlarmOverlayService) to prevent detection during high-pressure violations. (Oct27.10 - ISSUE-OCT28.16-F1).
*   **1.292 Constant Syntax Integrity (R-ID 762)**: Compile-time constants (`const val`) MUST use valid Kotlin literal syntax without unsupported escape sequences to ensure build reproducibility and prevent integration blocks. (Oct27.10 - ISSUE-OCT28.17-F1).
*   **1.293 Dynamic Buffer Test Integrity (R-ID 763)**: Instrumented audit tests verifying the forensic pipeline MUST dynamically query `ForensicSpillBuffer.capacity` to ensure test coverage across all storage health tiers and prevent unresolved reference failures during capacity transitions. (Oct27.10 - ISSUE-OCT28.20-F1).
*   **1.294 Startup Hydration Deferral (R-ID 764)**: To prevent Main-thread stalls (Davey events) and platform diagnostic triggers (StackLog) on budget hardware (A15), critical I/O-intensive startup tasks (R104 pruning and system network callback registration) MUST be deferred by at least 5 seconds after application entry. (Oct28.25 - ISSUE-OCT28.23-F1).

## 🛡️ Core Hardening Baseline
*   **SOT ID 869**: Startup Hydration Deferral - PASSED. Verified zero Davey events > 500ms and zero StackLog leaks during A15 startup hydration. (Oct28.25).
*   **SOT ID 868**: Audit Suite Recovery - PASSED. Verified compilation and execution of forensic tests after migrating to dynamic buffer capacity. (Oct27.10).
*   **SOT ID 867**: Constant Syntax Integrity - PASSED. (Oct27.10).
*   **SOT ID 866**: Stealth Suppression Authority - PASSED. (Oct27.10).
*   **SOT ID 865**: Badge Measurement Caching - PASSED. (Oct27.10).
*   **SOT ID 864**: Settings Restoration Policy - PASSED. (Oct27.10).
*   **SOT ID 863**: Settings Integrity Guard - PASSED. (Oct27.10).
*   **SOT ID 862**: Recovery Buffer Scaling - PASSED. (Oct27.10).
*   **SOT ID 861**: Pruning Telemetry Aggregation - PASSED. (Oct27.10).
*   **SOT ID 860**: Database Transaction Timeout - PASSED. (Oct27.10).
*   **SOT ID 859**: Pruning Worker Telemetry - PASSED. (Oct27.10).
*   **SOT ID 858**: Test Application Base Class Consolidation - PASSED. (Oct27.10).
*   **SOT ID 857**: Pruning Batch Optimization - PASSED. (Oct27.10).
*   **SOT ID 856**: Latency EMA Smoothing - PASSED. (Oct27.10).
*   **SOT ID 855**: IO Latency-Driven Throttle - PASSED. (Oct27.10).
*   **SOT ID 854**: Database Recovery Lock - PASSED. (Oct27.10).
*   **SOT ID 853**: Forensic SPI Level Audit - PASSED. (Oct27.10).
*   **SOT ID 852**: Forensic Recovery Integrity - PASSED. (Oct27.10).
*   **SOT ID 851**: Unit-Based Badge Scaling - PASSED. (Oct27.10).

## ✅ Verification Chapters
*   **Chapter 4.39 (Startup Hydration Audit)**: PASSED - Verified Davey < 500ms and zero StackLog leaks on Oct28.25 baseline.
*   **Chapter 4.38 (Audit Suite Recovery Audit)**: PASSED - Verified zero compilation errors and full test suite pass on Oct27.10 baseline.
*   **Chapter 4.37 (Syntax Integrity Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.36 (Stealth Hardening Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.35 (Badge Optimization Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.34 (Settings Restoration Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.33 (Settings Integrity Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.32 (Recovery Buffer Scaling Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.31 (Pruning Telemetry Aggregation Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.30 (Database Watchdog Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.29 (Pruning Telemetry Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.28 (Test Application Consolidation Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.27 (Pruning Optimization Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.26 (Latency EMA Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.25 (IO Throttling Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.24 (Recovery Phase Contention Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.23 (SPI Level Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.22 (Forensic Recovery Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.21 (UI Component Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.20 (Test Infrastructure Audit)**: PASSED. (Oct27.10).
*   **Chapter 4.19 (Forensic Convergence Audit)**: PASSED. (Oct27.10).
