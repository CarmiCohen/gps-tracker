# SOT Master Requirements & Hardening Status (Sep.28.9)

## 🏗️ Architectural Master Rules (56 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.42 Centralized Boot Lifecycle Authority (R521)**: Monotonic clock recovery and boot session validation must be centralized in a dedicated authority to unify logic scattered across background services and improve testability (Issue #1296/1355).
*   **1.43 Declarative Map Coordination (R522)**: The UI layer must remain declarative. Imperative map engine interactions (smoothing, camera triggers, overlay management) must be encapsulated in a dedicated `MapController` (Issue #1167).
*   **1.44 Build Pipeline Hardening (R523)**: The build system must ensure annotation processing stability across all variants by enabling `correctErrorTypes` and providing required UI tooling dependencies during stub generation to prevent `NonExistentClass` failures (Issue #1356).
*   **1.45 Legacy Component Pruning (R524)**: To maintain codebase hygiene, all decommissioned stubs and obsolete service entry points (e.g., TrackerService, ViewerService) must be strictly removed from the source tree once their responsibilities are fully migrated to unified components (Issue #1358).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 521**: Centralized Boot Lifecycle Authority - Extracted boot-session tracking and monotonic time recovery from `TimeProvider` and `AppAlarmManager` into a dedicated authority. (Resolved Sep.28.5).
*   **SOT ID 522**: Map Controller Decoupling - Extracted imperative osmdroid coordination and UI-side smoothing into `MapController`. (Resolved Sep.28.6).
*   **SOT ID 523**: Build Pipeline Hardening - Remediated kapt release-variant stub generation failures by enabling error type correction and harmonizing Compose tooling scopes. (Resolved Sep.28.8).
*   **SOT ID 524**: Legacy Component Pruning - Verified and audited the removal of decommissioned TrackerService and ViewerService stubs following the MonitorService unification. (Resolved Sep.28.9).

---

## 🏁 Verification Chapters
*   **Chapter 31.153 (Centralized Boot Lifecycle Authority)**: PASSED - Verified session invalidation across reboots. (Sep.28.5)
*   **Chapter 31.154 (Map Controller Decoupling)**: PASSED - Verified declarative boundary for Map UI. (Sep.28.6)
*   **Chapter 31.155 (Build Pipeline Hardening)**: PASSED - Verified successful `kaptReleaseKotlin` execution following dependency and kapt configuration adjustments. (Sep.28.8)
*   **Chapter 31.156 (Legacy Component Pruning)**: PASSED - Verified zero remaining references to legacy service stubs in the functional classpath. (Sep.28.9)
...
