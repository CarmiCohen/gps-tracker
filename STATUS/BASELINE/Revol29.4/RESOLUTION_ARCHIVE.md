# Resolution Archive

## ✅ Resolved Issues (Revol29.4)

### 1. ISSUE-REVOL29.4-S1: Multi-Module Build Stabilization
- **Symptoms**: Compilation errors in `:core-ui` (Kotlin/Compose mismatch), missing Hilt bindings in `:app-sensor`, and dependency conflicts in `:app-monitor`.
- **Root Cause**: Fragmented build configuration following the multi-module migration in Revol29.3.
- **Resolution**: Standardized Kotlin `1.9.24`, Compose Compiler `1.5.14`, and JVM `17` across all modules in `libs.versions.toml` and module-level `build.gradle.kts` files. Implemented `SensorModule.kt` to provide `TickOrchestrator` and `SensorDataStore` to Hilt.

### 2. ISSUE-REVOL29.4-F1: Forensic Audit Migration & Verification
- **Symptoms**: Forensic audit tests were stuck in the legacy `:app` module, preventing verification of the new architecture.
- **Root Cause**: Architecture shift required localized verification of `:app-sensor` and `:core-shared`.
- **Resolution**: Migrated and adapted `ForensicPersistenceAuditTest` and `ProductionReadinessAuditTest` to `:app-sensor`. Standardized `CryptoUtils.hashIdentity` to return 64-char hex strings (R-ID 765). Aligned `SensingEngine.getOptimalIntervalMs` to 1Hz for low battery (R-ID 16). Resolved `IllegalStateException` in tests by enforcing singleton DataStore usage. All 5 core forensic tests passed.

---

## ✅ Resolved Issues (Revol29.2)

### 1. ISSUE-REVOL29.2-F1: Sensing Engine Logic Implementation
- **Symptoms**: The `SensorService` bootstrap in the `:app-sensor` module was a skeleton lacking core sensing integration.
- **Root Cause**: The transition to the Revolution architecture required a ground-up implementation of the sensing pipeline to ensure zero-allocation and orientation-independent math.
- **Resolution**: Updated `SensorService.kt` to implement `SensorEventListener` for `TYPE_ACCELEROMETER` and `TYPE_GRAVITY`. Integrated `SensingEngine.rotateToWorldSpace` to ensure accurate horizontal/vertical decomposition regardless of device mounting. Implemented dynamic interval scaling via `SensingEngine.getOptimalIntervalMs` for thermal and battery protection. (R-ID 160, R-ID 16, R-ID 194).

---

## ✅ Resolved Issues (Oct27.10)

### 1. ISSUE-OCT28.23-F1: Forensic Audit Failure (Davey/StackLog)
- **Symptoms**: 838ms Davey event and StackLog leak at `AndroidNetworkProvider.kt:103` during A15 startup.
- **Root Cause**: High-pressure startup hydration sequence on budget hardware caused Main-thread I/O stalls and platform diagnostic triggers (StackLog).
- **Resolution**: Implemented stage-dependent deferral for R104 database pruning in `LogRepository` and system network callback registration in `AndroidNetworkProvider`. Added immediate state emission in the provider to avoid UI flicker during the deferral window. (R-ID 764).

### 2. ISSUE-OCT28.20-F1: Compilation Failure in Forensic Audit Suite
- **Symptoms**: Instrumented tests failed to compile due to `Unresolved reference: FORENSIC_SPILL_CAPACITY`.
- **Root Cause**: The transition to dynamic buffer scaling in `Oct27.10` removed the static capacity constant, which was still referenced by the audit tests.
- **Resolution**: Updated `ForensicConvergenceAuditTest.kt` and `ProductionReadinessAuditTest.kt` to use the dynamic `capacity` property from the injected `ForensicSpillBuffer` provider. (R-ID 763).

### 3. ISSUE-OCT28.20-F2: Forensic Audit Violation (Davey Events)
- **Symptoms**: Detected Davey events up to 1846ms during A15 startup, exceeding the forensic limit.
- **Resolution**: Enforced 50Hz (20ms) minimum forensic sampling interval in `EngineConstants.kt` to reduce main-thread I/O contention.

### 4. ISSUE-OCT28.17-F1: Compilation Failure in EngineConstants.kt
- **Symptoms**: The `:core:engine` module failed to compile with multiple "Expecting an expression" and "Property getter or setter expected" errors.
- **Root Cause**: String constants in `EngineConstants.kt` used invalid escaped quotes (`\"`) within `const val` declarations.
- **Resolution**: Removed invalid escape characters from all affected string constants. (R-ID 762).

### 5. ISSUE-OCT28.16-F1: Alarm Leakage on Tracker (Issue #1391)
- **Symptoms**: Audible sirens and red-screen overlays were manifesting locally on devices in Tracker mode during violations, compromising operational stealth.
- **Root Cause**: Manual test triggers in `UiEventCoordinator` and procedural audio paths in `AudioSynthesizer` lacked role-based suppression logic.
- **Resolution**: Implemented multi-layer stealth hardening across `AudioSynthesizer`, `UiEventCoordinator`, and `AlarmOverlayService`. (R-ID 761).
