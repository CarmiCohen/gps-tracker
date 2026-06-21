# Modern Software Engineering Methods & Tools (v8.9.10)

This document outlines the modern software engineering principles, architectural patterns, and toolchains utilized in the GPS Tracker project to ensure a robust, high-persistence native Android application.

## 1. Declarative UI (Jetpack Compose)
The project fully embraces **Jetpack Compose**, Google's modern toolkit for building native UI.

- **State-Driven Rendering**: The UI is a direct function of the `MainViewModel` state.
- **Performance State Optimization**: The application uses specialized primitive state holders like `mutableLongStateOf` and `mutableIntStateOf` for high-frequency telemetry updates. This eliminates object boxing overhead during rapid UI re-compositions.
- **Composition over Inheritance**: UI elements are built as small, reusable, and testable `@Composable` functions.
- **Ghost Mode UX (Issue 193)**: Implemented state-driven visual staleness indicators using dimmed color palettes (`Slate500`) for stale forensic data.

## 2. Unidirectional Data Flow (MVVM & Clean Architecture)
The project follows the **Model-View-ViewModel (MVVM)** architectural pattern combined with Clean Architecture principles.

- **ViewModel**: `MainViewModel.kt` centralizes business logic and telemetry processing. Decoupled into domain-specific UseCases.
- **Repository Pattern**: `MainRepository.kt` abstracts data storage.
- **Modularization**: Physical isolation of the Tracking Engine into `:core:engine` ensuring zero dependency leakage from the Android framework into the core logic.

## 3. Real-time Reactive Communication
- **Socket.io**: Persistent, event-driven WebSocket-based connection for P2P telemetry.
- **SharedFlow/StateFlow**: Reactive streams for internal data propagation and UI updates.
- **Acknowledged Events (Issue 194)**: Implemented a reliable, acknowledged sync loop for critical discrete events (like SIT) to prevent data loss.

## 4. Hardware-Level Native Integration
- **GNSS Monitoring**: Direct access to raw satellite SNR and constellation metadata for forensic signal analysis (`snrIdx`).
- **Sensor Fusion**: Advanced integration of Accelerometer, Barometer, and Magnetometer for physical security.
- **Hardware Revival (Issue 124/198)**: Automated hardware-level refresh cycles for stalled GPS chips.

## 5. Persistence & Self-Healing (Reliability Engineering)
- **Specialized Sticky Services**: `TrackerService` and `ViewerService` ensure high priority and role-specific execution.
- **WakeLock Hardening**: `SystemMonitor` implements non-reference-counted `PARTIAL_WAKE_LOCK` management with safety-timer refreshes.
- **AlarmManager Watchdogs**: Periodic system wakeups (`SYSTEM_WATCHDOG_INTERVAL_MS` 90s) to perform process health checks and self-healing restarts using exact alarms on Android 12+.

## 6. Toolchain & Modern Java/Kotlin
- **Kotlin 2.0+ / Java 17**: Modern JVM features and coroutines.
- **Dagger Hilt**: Standardized dependency injection.
- **ProGuard/R8**: Aggressive code shrinking and security obfuscation.
- **SDK 35**: Fully aligned with Android 15 edge-to-edge, Room migrations, and foreground service requirements.

## 7. Forensic Unification
As of v8.9.10, the forensic model is simplified and hardened. Legacy version tags have been removed from data models. Traceability is maintained at the emission layer, ensuring a clean and high-performance persistence architecture, further enhanced by **Power Forensic Parity** (Issue 192) and **Log Spatial Anchoring** (Issue 208).
