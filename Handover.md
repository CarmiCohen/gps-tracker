# Handover (Aug.18.08) - Diagnostic Phase: Performance Bottlenecks Isolated

## 🎯 Next Objective: Issue #208 & #207 - UI Layer & Main-Thread Audit
- **Goal**: Eliminate high-frequency object churn and main-thread hangs in `MainViewModel` and `AppMapContainer`.
- **Status**: 🔴 **OPEN (HIGH PRIORITY)**.
- **Context**: Diagnostic monitoring on Samsung A15 (SM-A155F) revealed that 1s+ frame hangs ("Davey" logs) persist even when forensic telemetry is throttled to 4Hz and hardware listeners to 5Hz. Logcat shows near-constant mark-compact GC cycles (~1/sec) taking 100ms+, indicating that the bottleneck is likely redundant list copying or object allocations in the UI state mapping logic (e.g., 1Hz pulses triggering full trail segment re-processing), not the tracking engine itself.

## 🧬 Forensic Pipeline Status (vAug.18.08)
The forensic telemetry system is architecturally hardened for production but currently runs in a **Diagnostic State** for stress-isolation:

### 1. Multi-Session Alignment (#203) - VERIFIED
*   **Buffer v3**: `ForensicSpillBuffer.kt` successfully migrated to absolute `Long` timestamps and `Double` coordinates in the 96-byte entry layout.
*   **Idempotency**: Signature-based deduplication in `LogRepository.performForensicDrain` verified via 1s lookback window. Monotonicity is guaranteed across service restarts and "dirty" reboots.

### 2. JNI & Memory Optimization (#202) - VERIFIED
*   **Zero-Churn Path**: `peekToEntities()` directly maps MappedByteBuffer data to Room `LogEntity` objects. 
*   **Result**: GC pressure from the tracking engine is negligible; current heap pressure (#208) is isolated to the UI/Compose layer mapping logic.

### 3. Diagnostic Stress Isolation (#204) - ACTIVE
*   **Throttled State**: Intervals in `EngineConstants.kt` are reduced to isolate CPU/IO load:
    - **Peak Fidelity**: 100Hz -> 4Hz (`250ms`).
    - **Power Aware**: 10Hz -> 2Hz (`500ms`).
*   **Hardware Scaling**: `AppSensorManager.kt` listeners (Linear Accel) set to `SENSOR_DELAY_NORMAL` (~5Hz).
*   **Isolation Finding**: Confirmed that the "stress" causing frame drops is independent of telemetry frequency, focusing the audit on the 1Hz UI pulse.

## 🛡️ Core Hardening (Aug.18.08 Resolutions)
*   **UI Artifact Remediation (#205)**: Wrapped technical overlays (`PhoneSetupOverlay`, `TrackerDashboard`) in forced LTR `CompositionLocalProvider` to resolve BiDi mirroring and punctuation artifacts (R205).
*   **Samsung Permission Hardening (#206)**: Implemented fallback in `MainActivity.kt` for `ACTION_MANAGE_OVERLAY_PERMISSION` to handle URI rejections on Samsung A15/API 35 (R206).

## 🛠️ Execution Sequence for Resumption
1.  **Profile UI Pulse**: Audit `MainViewModel` and `AppMapContainer` for O(N) operations or redundant object allocations triggered by the 1Hz `systemPulse`.
2.  **Optimize Overlays**: Gate marker and poly-line updates in `AppMapContainer` using `derivedStateOf` or stable keys to prevent full recompositions on every pulse.
3.  **Verify DB Transaction Locks**: Check if `LogRepository` drain operations are holding transaction locks that delay UI-thread database reads.
4.  **Restore Fidelity**: Once frame stability is achieved and GC pressure (#208) is resolved, revert `EngineConstants.kt` and `AppSensorManager.kt` to production 100Hz fidelity.

## 📊 Documentation State
- **RESOLUTION_ARCHIVE.md**: Updated to Section 68. Total unique resolutions: 648.
- **issues.md**: Synchronized to Aug.18.08. Issues #207 and #208 are primary targets.
- **SOT_MASTER_REQUIREMENTS.md**: Requirements R204, R205, and R206 added.
- **build.gradle**: VersionName: Aug.18.08.

vAug.18.08
