# SOT Master Requirements & Hardening Status (Sep.29.3)

## 🏗️ Architectural Master Rules (69 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted for brevity)
*   **1.58 Signaling Authority Centralization (R723/S554)**: Internal heartbeat timing logic MUST reside exclusively within the signaling transport layers (`ConnectivitySuite`) to preserve Service layer reactivity and simplify test coverage boundaries (Issue #1381).
*   **1.59 Siren UI Synchronization (R724/S556)**: Critical alarm sirens MUST be accompanied by a system notification and Red Screen overlay to ensure user visibility and provide a dismissal mechanism, preventing autonomous re-triggering due to unresolved states (Issue #1382).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 554**: Heartbeat Centralization - Moved bypass heartbeat loop to `ConnectivitySuite`, delegating telemetry mapping strictly to `AppEventCoordinator` logic. (Resolved Sep.29.3).
*   **SOT ID 555**: Documentation Version Alignment - Synchronized version headers across all tracking documents to Sep.29.3. (Resolved Sep.29.3).
*   **SOT ID 556**: Siren UI Synchronization - Integrated notification triggering into the siren requirement loop in `AppEventCoordinator` to resolve "Unstoppable Siren" loop. (Resolved Sep.29.3).

---

## 🏁 Verification Chapters
*   **Chapter 31.188 (Siren UI Synchronization Audit)**: PASSED - Verified that `AppEventCoordinator` now invokes `AppNotificationManager` alongside `AudioSynthesizer`, ensuring the Red Screen is visible for alarm dismissal. (Sep.29.3)
*   **Chapter 31.187 (Version Alignment Audit)**: PASSED - Verified all versioning strings in `app/build.gradle` and documentation headers are aligned at Sep.29.3. (Sep.29.3)
*   **Chapter 31.186 (Heartbeat Centralization)**: PASSED - Resolved Sep.29.3.
