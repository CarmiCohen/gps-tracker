# Architectural Evolution: Monolith to Modular High-Assurance (v8.9.54)

## 1. The Core Idea
The GPS Tracker app is a high-assurance tool where **reliability is forensic**. The primary problem identified was **Feature Corruption**: the app was a "Monolith" where math logic (GPS filtering), hardware management (sensors), and UI logic were tightly coupled in a single file.

**The Goal:** Ensure that changing a UI button or a network timeout cannot possibly break the core "Sit Detection" or "GPS Jump" logic.

## 2. The Concept: "The High-Assurance Vault"
We have transitioned to an architecture that treats the core tracking logic as a **Sacred Engine**.

### 2.1 The Components
- **The Logic Engine**: A "Pure" Kotlin/Java environment. It takes inputs (Lat/Lng, Accel, Baro) and produces outputs (Processed Point, Tamper Alert). It has no access to the database or Android UI.
- **The Forensic Bridge**: An interface (`LocationProcessorListener`) that decouples the engine from the side effects.
- **The Role-Based Services**: Instead of one service doing everything, we split the app into specific roles:
    - **Tracker Role**: Focused on battery, persistence, and sensor fidelity. Features 10Hz polling and escalated revival.
    - **Viewer Role**: Focused on HUD display, sparklines, and remote data sync. Includes background location and relative geofencing.

## 3. The Execution Plan

### Phase 1: Logic Decoupling
- **Refactor `LocationProcessor`**: Removed all dependencies on Android Context and UI-specific libraries.
- **Pure Data Models**: Introduced `EngineModels.kt` to ensure coordinates are handled as raw physics data.

### Phase 2: Service Specialization
- **Extracted `BaseMonitorService`**: Centralized plumbing (Wakelocks, watchdogs, heartbeat).
- **Split `AppService`**: Created `TrackerService` and `ViewerService`.

### Phase 3: Physical Module Isolation
- **Module Creation**: Moved the engine to `:core:engine`.
- **Strict Dependencies**: Enforced JVM-only purity for the engine to prevent framework leakage (Issue #100).

### Phase 4: Forensic & OEM Hardening
- **Monotonic Unification**: All logic uses monotonic time via `TimeProvider` (Issue #311).
- **Muzzle Window Implementation**: Implemented a 2000ms jitter suppression window (Issue #191).
- **OEM Fidelity**: Specialized 10Hz polling and escalated revival for Xiaomi and Samsung (Issue #363 / Issue #190).

### Phase 5: Domain Decoupling
- **UseCase Extraction**: Decoupled `MainViewModel.kt` into feature-specific UseCases (Issue #322).
- **State Synchronization**: Unified UI state management through domain layers.

### Phase 6: Forensic Simplification & UX Hardening (v8.9.54)
- **Model Purification**: Legacy version tags removed from data models.
- **Power Forensic Parity**: Achieved end-to-end parity for `currentMa` (Issue #337).
- **Ghost Mode UX**: Visual staleness indicators for stale forensic data > 15s (Issue #338). (Relaxed from 10s baseline in v8.9.54).
- **SIT Acknowledgment**: Reliable sync for discrete "sitting" events (Issue #194).
- **Log Spatial Anchor**: Every forensic event and alert is now geographically anchored for map reconstruction (Issue #208).

## 4. Expected Benefits
| Metric | Monolith (v6.8) | Modular (v8.9.54) |
| :--- | :--- | :--- |
| **Side-Effect Risk** | High (Everything touches everything) | Zero (Physically enforced isolation) |
| **Testability** | Requires Phone/Emulator | JVM Unit Tests (<2 seconds) |
| **Build Stability** | Fragile | Robust (Independent recompilation) |
| **Forensic Clarity** | Ambiguous | Full spatial traceability for all events |

---
**Status:** Architecture Hardened. Logic Physically Isolated. Forensic Integrity Verified. (v8.9.54 Baseline)
