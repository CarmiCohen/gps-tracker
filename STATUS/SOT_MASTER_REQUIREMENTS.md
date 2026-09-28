# SOT Master Requirements & Hardening Status (Sep.28.13)

## 🏗️ Architectural Master Rules (60 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.42 Centralized Boot Lifecycle Authority (R521)**: Monotonic clock recovery and boot session validation must be centralized in a dedicated authority to unify logic scattered across background services and improve testability (Issue #1296/1355).
*   **1.43 Declarative Map Coordination (R522)**: The UI layer must remain declarative. Imperative map engine interactions (smoothing, camera triggers, overlay management) must be encapsulated in a dedicated `MapController` (Issue #1167).
*   **1.44 Build Pipeline Hardening (R523)**: The build system must ensure annotation processing stability across all variants by enabling `correctErrorTypes` and providing required UI tooling dependencies during stub generation to prevent `NonExistentClass` failures (Issue #1356).
*   **1.45 Legacy Component Pruning (R524)**: To maintain codebase hygiene, all decommissioned stubs and obsolete service entry points (e.g., TrackerService, ViewerService) must be strictly removed from the source tree once their responsibilities are fully migrated to unified components (Issue #1358).
*   **1.46 Build Pipeline Dependency Pruning (R525)**: To optimize build speed and eliminate Java stub generation overhead, all modules must migrate from `kapt` to `KSP` for annotation processing (Room, Hilt) (Issue #1357).
*   **1.47 Temporal Logic Hardening (R526)**: To ensure logic consistency across deep-sleep transitions and enable high-fidelity behavioral testing, all background orchestration, backoff, and forensic measurement logic must exclusively use the centralized `TimeProvider` monotonic and wall-clock sources (Issue #1359).
*   **1.48 Signature Harmonization (R527)**: To ensure robust asynchronous cleanup of hardware hooks, all unregistration callbacks and lifecycle teardown paths across the app layer must perfectly harmonize with centralized TimeProvider references to eliminate lifecycle-driven runtime anomalies (Issue #1360).
*   **1.49 Forensic Reliability Precision (R528)**: The `LogRepository` MUST utilize `BigDecimal` fixed-point arithmetic for the forensic reliability EMA to prevent cumulative precision loss during high-frequency trace bursts and ensure alert consistency (Issue #1361).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 521**: Centralized Boot Lifecycle Authority - Extracted boot-session tracking and monotonic time recovery from `TimeProvider` and `AppAlarmManager` into a dedicated authority. (Resolved Sep.28.5).
*   **SOT ID 522**: Map Controller Decoupling - Extracted imperative osmdroid coordination and UI-side smoothing into `MapController`. (Resolved Sep.28.6).
*   **SOT ID 523**: Build Pipeline Hardening - Remediated kapt release-variant stub generation failures by enabling error type correction and harmonizing Compose tooling scopes. (Resolved Sep.28.8).
*   **SOT ID 524**: Legacy Component Pruning - Verified and audited the removal of decommissioned TrackerService and ViewerService stubs following the MonitorService unification. (Resolved Sep.28.9).
*   **SOT ID 525**: Build Pipeline Dependency Pruning - Migrated the build pipeline from kapt to KSP for Room and Hilt, improving performance and architectural simplicity. (Resolved Sep.28.10).
*   **SOT ID 526**: Temporal Logic Hardening - Unified all service and connectivity logic gates under the centralized `TimeProvider` authority. (Resolved Sep.28.11).
*   **SOT ID 527**: Signature Harmonization - Remediated mismatched unregistration signatures in production listeners by enforcing TimeProvider integration across all hardware suite and network provider teardown scopes. (Resolved Sep.28.12).
*   **SOT ID 528**: Forensic Reliability Math Hardening - Migrated reliability EMA accumulator to BigDecimal to ensure absolute precision under extreme burst loads. (Resolved Sep.28.13).

---

## 🏁 Verification Chapters
*   **Chapter 31.153 (Centralized Boot Lifecycle Authority)**: PASSED - Verified session invalidation across reboots. (Sep.28.5)
*   **Chapter 31.154 (Map Controller Decoupling)**: PASSED - Verified declarative boundary for Map UI. (Sep.28.6)
*   **Chapter 31.155 (Build Pipeline Hardening)**: PASSED - Verified successful `kaptReleaseKotlin` execution following dependency and kapt configuration adjustments. (Sep.28.8)
*   **Chapter 31.156 (Legacy Component Pruning)**: PASSED - Verified zero remaining references to legacy service stubs in the functional classpath. (Sep.28.9)
*   **Chapter 31.157 (Build Pipeline Dependency Pruning)**: PASSED - Verified successful `:app:assembleDebug` execution following KSP migration for Room and Hilt. (Sep.28.10)
*   **Chapter 31.158 (Temporal Logic Hardening)**: PASSED - Verified zero remaining direct `SystemClock` or `System.currentTimeMillis()` calls in core service orchestration via grep audit. (Sep.28.11)
*   **Chapter 31.159 (Signature Harmonization)**: PASSED - Verified successful compilation and clean signature mapping across all managed unregistration contexts via `:app:assembleDebug`. (Sep.28.12)
*   **Chapter 31.160 (Forensic Reliability Math Hardening)**: PASSED - Verified precision stability via high-frequency burst simulation and clean compilation. (Sep.28.13)
...
