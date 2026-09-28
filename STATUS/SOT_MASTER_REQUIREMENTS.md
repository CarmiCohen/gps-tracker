# SOT Master Requirements & Hardening Status (Sep.28.7)

## 🏗️ Architectural Master Rules (54 Rules)

### 1. Lifecycle & Resource Management
*   ...
*   **1.42 Centralized Boot Lifecycle Authority (R521)**: Monotonic clock recovery and boot session validation must be centralized in a dedicated authority to unify logic scattered across background services and improve testability (Issue #1296/1355).
*   **1.43 Declarative Map Coordination (R522)**: The UI layer must remain declarative. Imperative map engine interactions (smoothing, camera triggers, overlay management) must be encapsulated in a dedicated `MapController` (Issue #1167).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 521**: Centralized Boot Lifecycle Authority - Extracted boot-session tracking and monotonic time recovery from `TimeProvider` and `AppAlarmManager` into a dedicated authority, simplifying the core temporal model. (Resolved Sep.28.5).
*   **SOT ID 522**: Map Controller Decoupling - Extracted imperative osmdroid coordination and UI-side smoothing into `MapController`, enforcing a declarative boundary for the map UI layer. (Resolved Sep.28.6).
...

## 🏁 Verification Chapters
*   **Chapter 31.153 (Centralized Boot Lifecycle Authority)**: PASSED - Verified that `BootLifecycleAuthority` correctly identifies session invalidation across reboots and manages monotonic time recovery. `TimeProvider` flattened to basic temporal methods. (Sep.28.5)
*   **Chapter 31.154 (Map Controller Decoupling)**: PASSED - Verified that `MapComponents.kt` is now purely declarative, delegating all imperative `MapView` and `MapOverlayManager` logic to `MapController`. (Sep.28.6)
...
