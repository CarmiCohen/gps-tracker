# SOT Master Requirements (Sep.02.46)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (40 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.12 Hardware Disposal (R887/R888/R889/R890/R891)**: **MANDATORY**. All hardware callbacks MUST be unregistered using the `ManagedHardware` synchronization pattern. To eliminate native `BaseEventQueue` leaks on budget hardware (SM-A155F), the unregistration MUST follow a strict sequence: Location and GNSS updates MUST be removed *before* sensors and display listeners. `HardwareProvider.stop()` MUST implement an 800ms settling window after all unregistrations are confirmed but *before* the internal `HandlerThread` is terminated (R891). All unregistrations MUST utilize the `ManagedUnregistrationHelper` with a 4000ms latch and forensic duration logging (R889). (Updated Sep.01.24).
*   **1.13 WorkManager Initialization (R892)**: **MANDATORY**. WorkManager MUST be manually initialized in `GpsApplication.onCreate()` to support custom `HiltWorkerFactory` and avoid `IllegalStateException` during boot-triggered background starts (Sep.01.24).
*   **1.14 Context Shadowing (R894)**: **MANDATORY**. High-frequency system service interactions MUST use the `ContextShadow` delegate to bypass internal framework IPC lookups for the package name, eliminating diagnostic log spam. Coverage expanded to all core managers and utilities (Sep.02.43).
*   **1.15 Looper Alignment (R893)**: **MANDATORY**. All `ManagedNetworkCallback` and `FusedLocationProvider` registrations MUST specify the `MainLooper` to ensure alignment with the synchronous teardown logic, preventing native `BaseEventQueue` disposal failures (Sep.01.27).
*   **1.16 16KB Page Alignment (R895)**: **MANDATORY**. Native libraries MUST be aligned to 16KB boundaries to support Android 15+ devices. Implementation requires AGP 8.3+, `useLegacyPackaging = false` in `app/build.gradle`, and `-Wl,-z,max-page-size=16384` linker flags (Sep.02.27). Verified in #118 (Sep.02.46).
*   **1.17 Robust Battery Navigation (R896)**: **MANDATORY**. Battery optimization exemption intents MUST use `Uri.fromParts("package", pkg, null)` for URI encoding and implement a multi-tier fallback: (1) `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, (2) `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, and (3) `ACTION_APPLICATION_DETAILS_SETTINGS` to ensure accessibility on Samsung/Xiaomi devices (Sep.02.40).

## 🧩 Functional Requirements (199 IDs)
*   **R-ID 172 (Forensic Parity)**: SIT events MUST include high-precision timestamps (`sitVzTs`, `sitVzRt`) captured at the moment of peak vertical velocity to ensure forensic traceability across all telemetry layers (Sep.02.46).
*   **R-ID 197 (Forensic Teardown)**: Teardown logic MUST include forensic timing logs for each component's unregistration to identify OS-level disposal delays.
*   **R-ID 198 (Dynamic Sensitivity Propagation)**: UI-driven sensor sensitivity adjustments (Vibration, Tilt) MUST be propagated to the tracking engine's `SentinelValidator` to replace hardcoded constants with dynamic user-defined thresholds (R2.3). (Sep.02.41).
*   **R-ID 199 (Telemetry Observation Parity)**: `MainViewModel` MUST observe both `localLocation` and `trackerLocation` repository flows to ensure HUD and Dashboard telemetry remains live regardless of app role or hardware version (R3.1). (Sep.02.42).

*(Total: 40 Architectural Rules + 199 Functional R-IDs = 239 Items)*
