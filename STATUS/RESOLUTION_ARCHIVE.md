# Project Resolution Archive (Sep.19.13)

## 🟢 Sep.19.13
*   **Excessive WakeLock Acquisition in Activity-Denied Scenarios (#1118)**: Resolved excessive battery drain in `HardwareSuite.kt` where the system would poke a WakeLock every 10 seconds if Step Detector registration failed, even if the failure was due to user-denied permissions. Implemented a permission check for `ACTIVITY_RECOGNITION` before poking the WakeLock, ensuring the fallback is suppressed when explicitly restricted by the user (R-ID 372).

## 🟢 Sep.19.12
*   **Uncontrolled Sensor Registration in setPowerSaveMode Race (#1117)**: Resolved a race condition in `HardwareSuite.kt` where `setPowerSaveMode` could re-register sensors on a stopped suite. Added an explicit `isStarted.get()` check within the posted handler block to ensure sequential lifecycle integrity during rapid mode transitions (R-ID 371).

## 🟢 Sep.19.11
*   **Acoustic Monitor Resource Race on Rapid Restart (#1116)**: Resolved a resource race condition in `HardwareSuite.kt` where rapid restarts could cause multiple threads to compete for the `AudioRecord` resource. Implemented `acousticLock` and mandatory thread joining in `startAcousticMonitoring()`, ensuring that any previous monitor session is definitively terminated before a new one initializes (R-ID 370).

## 🟢 Sep.19.10
*   **Stale Forensic and SNR Buffers across Suite Lifecycle (#1115)**: Resolved an issue in `HardwareSuite.kt` where circular buffers (`sensorBuffer`, `snrBuffer`, `logicSnapshotBuffer`, `forensicSnapshotBuffer`) and the `lastBufferRecordRt` timestamp were not cleared during suite termination. By explicitly resetting these structures in `stop()`, the system now guarantees a clean forensic state for every service session restart, preventing stale data from polluting new monitoring cycles (R-ID 369).

## 🟢 Sep.19.09
*   **HardwareSuite Thread-Safety and Visibility Vulnerabilities (#1114)**: Resolved memory visibility and race conditions in `HardwareSuite.kt` snapshotting logic. Applied `@Volatile` to high-frequency shared state variables (lux, acousticDb, tilt, velocity, etc.) to ensure correct cross-thread reads during forensic audits. Unified the synchronization strategy by wrapping both the sensor update handlers and the peak-reset snapshot consumption methods (`consumeLogicSnapshot`, `consumeForensicSnapshot`) in `synchronized(this)`, guaranteeing atomic "read-and-reset" operations under high system load (R-ID 368).

## 🟢 Sep.19.08
*   **Forensic Multi-Role Integrity Hardening (#1113)**: Resolved state collision in `ForensicAuditor` by implementing role-based (`T` for Tracker, `V` for Viewer) state tracking using a `ConcurrentHashMap`. Each role now maintains its own stability audit counters, GNSS jitter peaks, and sensor rate audit flags, ensuring accurate forensic reporting when both services run concurrently on the same device (R-ID 367).
*   **Shared Sensor Rate Audit Flag Persistence (#1119)**: Decoupled the `isSensorRateAudited` flag in `ForensicAuditor` by moving it into the role-specific `RoleState` objects. This allows both Tracker and Viewer roles to complete their respective sensor rate efficacy audits independently.

## 🟢 Sep.19.07
*   **Incomplete Reset in resetServiceTimers (HardwareSuite State Persistence) (#1112)**: Updated both `TrackerService.kt` and `ViewerService.kt` to call `hardwareSuite.resetBaseline()` within the `resetServiceTimers()` method. This ensures that all internal hardware states, including IMU peak values, adaptive floors, and GNSS revival flags, are properly zeroed when a session is terminated or reset (R-ID 366).

## 🟢 Sep.19.06
*   **Proximity Suppression Lock-in due to Hysteresis Persistence (#1111)**: Implemented temporal decay for the display flickering suppression logic in `HardwareSuite.kt`. By checking if the last display transition occurred within `DISPLAY_FLICKER_TIMEOUT_MS` (3s), the system now allows proximity "Far" transitions once flickering ceases, even without a further display event. This prevents suppression "lock-in" on stationary devices. Ensured state reset in `stop()` and `resetBaseline()` (R-ID 365).

## 🟢 Sep.19.05
*   **Initialization Race in HardwareSuite.start() causing False GPS Gap (#1110)**: Reordered the state initialization block in `HardwareSuite.start()`. By initializing `sessionStartRt`, `lastBaroZeroingRt`, and `lastFixRt` before `isStarted` is flipped to `true`, the background audit thread can no longer run an audit update loop with non-initialized/zeroed parameters, avoiding false GPS gaps and incorrect battery baselines on session initialization (R-ID 364).

## 🟢 Sep.19.04
*   **Resource Leak in GNSS Revival Burst during Safe Mode Transition (#1109)**: Refactored GNSS revival pulse logic in `HardwareSuite.kt` to use a `try-finally` block within a single coroutine. This guarantees that Raw and Fused location listeners are always unregistered upon burst completion or cancellation (e.g., during Safe Mode transition or suite shutdown). The refactoring enabled the removal of the redundant `revivalBurstJob` state variable, simplifying the class's resource management (R-ID 363).

## 🟢 Sep.19.03
*   **Redundant Battery Baseline Capture in Background/Idle State (#1108)**: Fully resolved the redundant battery baseline capture logic by initializing `lastFixRt` to `sessionStartRt` inside both `start()` and `resetBaseline()`. This guarantees a proper grace period before any GNSS gap or stall can be declared, preventing immediate redundant baseline captures upon app activation or reset (R-ID 361).

## 🟢 Sep.19.02
*   **GNSS Stall Timing Leakage during Suite Inactivity (#1107)**: Resolved a critical logic flaw in `HardwareSuite.kt` where the background audit loop was accumulating GNSS stall duration (`pendingEnterRt`) even while the suite was inactive. Guarded the background coroutine with `isStarted.get()` and implemented an explicit reset of revival state variables in `stop()` and `resetBaseline()`. This prevents immediate `HardwareLock` triggers upon app activation after prolonged indoor idle periods (R-ID 360).
*   **Redundant Battery Baseline Capture in Background/Idle State (#1108)**: Optimized power consumption by preventing `ForensicAuditor.captureRevivalStart()` from being triggered during app initialization or idle states. The capture is now strictly gated by the suite's active lifecycle (R-ID 361).
*   **Resource Leak in GNSS Revival Burst during Safe Mode (#1109)**: Modified `setSafeMode(active)` in `HardwareSuite.kt` to explicitly unregister `rawRevivalListener` and `revivalCallback` when Safe Mode is enabled. This ensures that the raw GPS provider and high-accuracy fused updates are immediately terminated if Safe Mode is toggled during a 10-second burst, preventing unintended battery drain (R-ID 362).
*   **Energy Footprint Data Loss following Intermediate Audit Consumption (#1106)**: Modified `ForensicAuditor.computeEnergyFootprint()` to support non-destructive peeking via a `consume` parameter. Updated `HardwareSuite.kt` to use `consume = false` during intermediate `HardwareLock` events, ensuring the final `Success` event retains the full battery baseline for accurate total-cycle energy reporting.

## 🟢 Sep.19.01
*   **Battery Baseline Capture Persistence across Lifecycle Transitions (#1105)**: Resolved an issue in `HardwareSuite.kt` where the `revivalBaselineCaptured` flag was not reset to `false` when the suite was stopped or when its baseline was reset. The fix ensures the flag is properly cleared in `stop()` and `resetBaseline()`, allowing fresh battery baselines to be recorded for subsequent tracking sessions (R-ID 359).

*(All other resolved issues have been successfully moved to the Resolution Archive file).*
