# SOT Master Requirements (Sep.01.26)

This document defines the Source of Truth (SOT) for all high-assurance logic, architectural standards, and forensic requirements.

## 🏗️ Architectural Master Rules (37 Rules)

### 1. Lifecycle & Resource Management
*   **1.1 Context权威 (R001)**: **MANDATORY**. Use `ApplicationContext` for all singleton services. Activity context is strictly for UI-only components.
*   **1.12 Hardware Disposal (R887/R888/R889/R890/R891)**: **MANDATORY**. All hardware callbacks MUST be unregistered using the `ManagedHardware` synchronization pattern. To eliminate native `BaseEventQueue` leaks on budget hardware (SM-A155F), the unregistration MUST follow a strict sequence: Location and GNSS updates MUST be removed *before* sensors and display listeners. `HardwareProvider.stop()` MUST implement an 800ms settling window after all unregistrations are confirmed but *before* the internal `HandlerThread` is terminated (R891). All unregistrations MUST utilize the `ManagedUnregistrationHelper` with a 4000ms latch and forensic duration logging (R889). (Updated Sep.01.24).
*   **1.13 WorkManager Initialization (R892)**: **MANDATORY**. WorkManager MUST be manually initialized in `GpsApplication.onCreate()` to support custom `HiltWorkerFactory` and avoid `IllegalStateException` during boot-triggered background starts (Sep.01.24).
*   **1.14 Context Shadowing (R894)**: **MANDATORY**. High-frequency system service interactions MUST use the `ContextShadow` delegate to bypass internal framework IPC lookups for the package name, eliminating diagnostic log spam (Sep.01.26).

## 🧩 Functional Requirements (197 IDs)
*   **R-ID 197 (Forensic Teardown)**: Teardown logic MUST include forensic timing logs for each component's unregistration to identify OS-level disposal delays.

*(Total: 37 Architectural Rules + 197 Functional R-IDs = 234 Items)*
