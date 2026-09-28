# 🏛️ Resolution Archive - Sep.28.10

## 🏁 Issue #1357: Build Pipeline Dependency Pruning (KSP Migration)
*   **Resolved**: Sep.28.10
*   **Root Cause**: The project relied on `kapt` (Kotlin Annotation Processing Tool) for Room and Hilt, which introduces significant build overhead due to Java stub generation. This slowed down compilation and created risks of classpath resolution conflicts, especially in release variants.
*   **Remediation**:
    *   **libs.versions.toml**: Added KSP plugin version `1.9.23-1.0.19` synchronized with the project's Kotlin version.
    *   **build.gradle (root)**: Integrated the KSP plugin into the top-level build configuration and advanced the global `versionName` to `Sep.28.10`.
    *   **app/build.gradle**: Migrated from `kotlin-kapt` to `com.google.devtools.ksp`. Replaced all `kapt` dependency configurations with `ksp` for Room and Hilt compilers.
    *   **Optimization**: Excised the legacy `kapt` configuration block, reducing build script complexity.
*   **Significance**: Low (Build Speed & Architectural Modernization).
*   **SOT ID**: 525 (Build Pipeline Dependency Pruning)

## 🏁 Issue #1358: Codebase Leftovers Pruning & Version Hardening
*   **Resolved**: Sep.28.9
*   **Root Cause**: Following the unification of background orchestration into `MonitorService`, the decommissioned `TrackerService.kt` and `ViewerService.kt` files remained as empty leftover stubs in the tree. This violated strict code cleanliness and build optimization policies.
*   **Remediation**:
    *   **Source Code**: Permanently removed the obsolete `TrackerService.kt` and `ViewerService.kt` files from the source tree.
    *   **Version Baseline**: Advanced the project and subproject version configurations to `Sep.28.9` and verified compliance with the unified `verifyProjectIntegrity` task.
*   **Significance**: Low (Codebase Hygiene & Architecture Hardening).
*   **SOT ID**: 524 (Legacy Component Pruning)

## 🏁 Issue #1356: kaptReleaseKotlin Stub Generation Hardening
*   **Resolved**: Sep.28.8
*   **Root Cause**: During the release build type processing (`kaptReleaseKotlin`), the Kotlin Annotation Processing Tool (kapt) failed to resolve Compose `@Preview` annotations or dependencies on the release classpath. This led to `NonExistentClass` placeholders in generated stubs, causing subsequent Java compilation to fail.
*   **Remediation**:
    *   **app/build.gradle**: Enabled `correctErrorTypes = true` in the `kapt` configuration block to allow the build to proceed even with unresolved types in stubs.
    *   **app/build.gradle**: Promoted `libs.androidx.ui.tooling.preview` to `implementation` to ensure its presence on the classpath during all build variants' annotation processing phases.
*   **Significance**: Medium (Release Pipeline Stability).
*   **SOT ID**: 523 (Build Pipeline Hardening)

## 🏁 Issue #1354: Gradle Task Deduplication
*   **Resolved**: Sep.28.7
*   **Root Cause**: Multiple custom verification and synchronization tasks (`verifyVersionIntegrity`, `syncDocsVersion`, `verifyInterfaceBindings`) were registered individually and attached to the `preBuild` phase of all subprojects. This resulted in redundant task configuration, increased build graph complexity, and slower project evaluation.
*   **Remediation**:
    *   **build.gradle**: Consolidated the three distinct tasks into a single unified task named `verifyProjectIntegrity`.
    *   **Optimization**: Merged version type-safety audits, Hilt interface binding validation, and automated documentation version synchronization into a single atomic execution unit.
    *   **Lifecycle Hook**: Updated the `subprojects` configuration to depend exclusively on `verifyProjectIntegrity` during the `preBuild` phase, significantly reducing the overhead of architectural audits during local development and CI runs.
*   **Significance**: Low (Build Speed & Maintenance Optimization).

## 🏁 Issue #1167: Map Overlay Imperative to Declarative Controller
*   **Resolved**: Sep.28.6
*   **Root Cause**: The `OsmMap` composable was overloaded with imperative `LaunchedEffect` triggers and direct handling of `MapOverlayManager` side effects, breaching the declarative boundaries of Compose UI and containing redundant coordinate smoothing already computed at the state mapping layer.
*   **Remediation**:
    *   **MapController.kt**: Introduced a dedicated imperative coordinator class to isolate `MapView` layout state updates, asynchronous caching overlays, follow-modes, and camera animations.
    *   **MapComponents.kt**: Re-factored `OsmMap` into a clean declarative wrapper that delegates platform map operations to `MapController`. Excised redundant UI-side EMA filters in favor of `UiStateCoordinator` authority values.
    *   **Validation**: Built and compiled with full regression verification.

## 🏁 Issue #1355: TimeProvider Cleanup
*   **Resolved**: Sep.28.5
*   **Root Cause**: The architectural extraction of the Centralized Boot Lifecycle Authority in the previous step left legacy session-tracking expectations on the temporal layer. `TimeProvider` needed to be strictly flattened to simple monotonic and wall-clock contracts to guarantee pure separation of concerns.
*   **Remediation**:
    *   **TimeProvider.kt**: Enforced basic, streamlined temporal methods (`currentTimeMillis`, `elapsedRealtime`), completely eliminating lingering platform boot-session or identifier footprints from the interface signature.
    *   **ServiceBehaviorAuditTest.kt**: Patched a compile-time signature regression in the test suite by correctly supplying the contextual `activityType` parameter to `calculateGpsInterval` calls, restoring test suite integrity.
    *   **Validation**: Successfully executed the entire test runner suite across all modules, proving zero logic regressions or compilation leakage.

## 🏁 Issue #1296: Centralized Boot Lifecycle Authority
*   **Resolved**: Sep.28.4
*   **Root Cause**: Monotonic clock recovery and boot session validation logic were scattered across the codebase and embedded inside `TimeProvider` and background components like `AppAlarmManager`, which impeded clean platform abstraction and background-service testability.
*   **Remediation**:
    *   **BootLifecycleAuthority.kt**: Defined a brand-new interface contract in `:core:engine` specifying session validation and monotonic clock recovery methods.
    *   **AndroidBootLifecycleAuthority.kt**: Implemented the Android-specific contract using the localized `/proc/sys/kernel/random/boot_id` source.
    *   **AppAlarmManager.kt**: Refactored to delegate boot session tracking, invalidation handling, and logic state cleanups to the centralized `BootLifecycleAuthority`.
    *   **Hilt Hooking**: Configured build-time interface analysis bindings for the new authority to prevent runtime injection issues.
*   **SOT ID**: 521 (Centralized Boot Lifecycle Authority)

## 🏁 Issue #1294: Build-Time Interface Validation
*   **Resolved**: Sep.28.3
*   **Root Cause**: Lack of static verification for Hilt module bindings created a risk of runtime `ProvisionException` errors when core interfaces defined in `:core:engine` lacked implementations in the `:app` module.
*   **Remediation**:
    *   **build.gradle**: Implemented `verifyInterfaceBindings` custom Gradle task.
    *   **Static Analysis**: The task parses `AppModule.kt` and `PowerModule.kt` for `: Interface` binding patterns using regex, ensuring `TimeProvider`, `PowerStateProvider`, `NetworkProvider`, `SignalingTransport`, `SystemStatusProvider`, and `SignalingProvider` are correctly bound.
    *   **Lifecycle Hook**: Integrated the task into the `preBuild` phase of all subprojects, ensuring the build fails immediately if architectural consistency is violated.
*   **SOT ID**: 520 (Build-Time Interface Validation)

## 🏁 Issue #1353: Unified Activity Context Provider
*   **Resolved**: Sep.28.2
*   **Root Cause**: User activity detection (API-based and heuristic) was tightly coupled within `HardwareSuite.kt`, increasing the complexity of the hardware management layer and making activity logic difficult to test or modify in isolation.
*   **Remediation**:
    *   **ActivityContextProvider.kt**: Created a dedicated, thread-safe singleton to encapsulate Google Play Services Activity Recognition and the GPS speed/vibration heuristic fallback mechanism.
    *   **HardwareSuite.kt**: Refactored to inject `ActivityContextProvider`. Removed internal BroadcastReceiver, activity recognition request logic, and redundant heuristic state. Offloaded activity heuristic updates to the provider during the sensor tick.
    *   **Architecture**: Decoupled high-level activity context from low-level sensor management, improving modularity and testability.
*   **SOT ID**: 519 (Unified Activity Context Provider)
