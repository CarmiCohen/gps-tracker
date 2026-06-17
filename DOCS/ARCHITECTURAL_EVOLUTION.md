# Architectural Evolution: Monolith to Modular High-Assurance (v8.8.35)

## 1. The Core Idea
The GPS Tracker app is a high-assurance tool where **reliability is forensic**. The primary problem identified was **Feature Corruption**: the app was a "Monolith" where math logic (GPS filtering), hardware management (sensors), and UI logic were tightly coupled in a single file (`AppService.kt`).

**The Goal:** Ensure that changing a UI button or a network timeout cannot possibly break the core "Sit Detection" or "GPS Jump" logic.

## 2. The Concept: "The High-Assurance Vault"
We have transitioned to an architecture that treats the core tracking logic as a **Sacred Engine**.

### 2.1 The Components
- **The Logic Engine**: A "Pure" Kotlin/Java environment. It takes inputs (Lat/Lng, Accel, Baro) and produces outputs (Processed Point, Tamper Alert). It has no access to the database or Android UI.
- **The Forensic Bridge**: An interface (`LocationProcessorListener`) that decouples the engine from the side effects.
- **The Role-Based Services**: Instead of one service doing everything, we split the app into specific roles:
    - **Tracker Role**: Focused on battery, persistence, and sensor fidelity.
    - **Viewer Role**: Focused on HUD display, sparklines, and remote data sync.

## 3. The Execution Plan

### Phase 1: Logic Decoupling (Completed)
- **Refactor `LocationProcessor`**: Removed all dependencies on `MainRepository`, `Context`, and UI-specific GeoPoint libraries.
- **Pure Data Models**: Introduced `EngineModels.kt` to ensure coordinates are handled as raw physics data, not UI objects.
- **Callback Architecture**: The service now "listen" to the engine's findings rather than the engine "telling" the database what to do.

### Phase 2: Service Specialization (Completed)
- **Extracted `BaseMonitorService`**: Centralized the "plumbing" (Wakelocks, network watchdogs, heartbeat).
- **Split `AppService`**: Created `TrackerService` and `ViewerService`.

### Phase 3: Physical Module Isolation (Completed)
- **Module Creation**: Moved the refactored engine to `:core:engine`.
- **Strict Dependencies**: The `:app` module depends on `:core:engine`, but the Engine is physically unable to see any code in `:app`.
- **Build Enforcement**: `:core:engine` is a pure `java-library` (JVM-only). This prevents accidental introduction of Android framework dependencies.

### Phase 4: Forensic & OEM Hardening (Completed)
- **Monotonic Unification**: All engine logic (cooldowns, gaps, signal loss) now uses monotonic time via `TimeProvider`.
- **Muzzle Window Implementation**: Implemented a 500ms jitter suppression window to resolve physical tamper race conditions during synchronization.
- **OEM Fidelity**: Specialized 10Hz polling and Autostart verification for Xiaomi and Samsung high-assurance deployments.
- **Architectural Standardization**: Final synchronization of all physics and network thresholds with the System Source of Truth.

### Phase 5: Domain Decoupling (Completed)
- **UseCase Extraction**: The `MainViewModel` was decoupled into feature-specific UseCases (Navigation, Settings, Telemetry, Behavior, Session, Alert, Map).
- **State Synchronization**: Unified UI state management through domain layers, eliminating "God Object" side-effects and improving reactivity.

### Phase 6: Forensic Simplification (Completed - v8.8.35)
- **Model Purification**: Legacy version tags (`ver`, `vid`) formally removed from data models and database schemas. The system maintains traceability by injecting the version string only at the emission layer (Relay/LogSink), ensuring a lean and maintainable data structure.

## 4. Expected Benefits
| Metric | Monolith (v6.8) | Modular (v8.8.35) |
| :--- | :--- | :--- |
| **Side-Effect Risk** | High (Everything touches everything) | Zero (Physically enforced isolation) |
| **Testability** | Requires Phone/Emulator | JVM Unit Tests (<2 seconds) |
| **Build Stability** | Fragile | Robust (Independent recompilation) |
| **Code Clarity** | 700+ line "God Classes" | Small, single-purpose classes |

---
**Status:** Architecture Hardened. Logic Physically Isolated. Forensic Integrity Verified. (v8.8.35 Baseline)
