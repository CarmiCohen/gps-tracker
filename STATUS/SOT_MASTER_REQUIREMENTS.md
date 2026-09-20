# SOT Master Requirements & Hardening Status (Sep.19.13)

## 🛡️ Core Hardening Baseline
*   **SOT ID 372**: Activity-Denied WakeLock Fallback Hardening - Resolved excessive battery drain in `HardwareSuite.kt` where the system would poke a WakeLock every 10 seconds if Step Detector registration failed, even if the failure was due to user-denied permissions. Implemented a permission check for `ACTIVITY_RECOGNITION` before poking the WakeLock, ensuring the fallback is suppressed when explicitly restricted by the user (R-ID 372). (Resolved Sep.19.13)
*   **SOT ID 371**: Asynchronous Sensor Registration Hardening - Resolved a race condition in `HardwareSuite.kt` where `setPowerSaveMode` could re-register sensors on a stopped suite. Added an explicit `isStarted.get()` check within the posted handler block to ensure sequential lifecycle integrity during rapid mode transitions (R-ID 371). (Resolved Sep.19.12)
*   **SOT ID 370**: Acoustic Monitor Lifecycle Hardening - Resolved a resource race condition in `HardwareSuite.kt` where rapid restarts could cause multiple threads to compete for the `AudioRecord` resource. Implemented `acousticLock` and mandatory thread joining in `startAcousticMonitoring()`, ensuring that any previous monitor session is definitively terminated before a new one initializes (R-ID 370). (Resolved Sep.19.11)
*   **SOT ID 369**: Stale Forensic Buffer Lifecycle Hardening - Resolved an issue in `HardwareSuite.kt` where circular buffers (`sensorBuffer`, `snrBuffer`, `logicSnapshotBuffer`, `forensicSnapshotBuffer`) and the `lastBufferRecordRt` timestamp were not cleared during suite termination. By explicitly resetting these structures in `stop()`, the system now guarantees a clean forensic state for every service session restart, preventing stale data from polluting new monitoring cycles (R-ID 369). (Resolved Sep.19.10)
*   **SOT ID 368**: Snapshot Thread-Safety Hardening - Resolved memory visibility and race conditions in `HardwareSuite.kt` snapshotting logic. Applied `@Volatile` to high-frequency shared state variables (lux, acousticDb, tilt, velocity, etc.) to ensure correct cross-thread reads during forensic audits. Unified the synchronization strategy by wrapping both the sensor update handlers and the peak-reset snapshot consumption methods (`consumeLogicSnapshot`, `consumeForensicSnapshot`) in `synchronized(this)`, guaranteeing atomic "read-and-reset" operations under high system load (R-ID 368). (Resolved Sep.19.09)
*   **SOT ID 367**: Forensic Multi-Role Integrity Hardening - Resolved state collision in `ForensicAuditor` by implementing role-based (`T` for Tracker, `V` for Viewer) state tracking using a `ConcurrentHashMap`. Each role now maintains its own stability audit counters, GNSS jitter peaks, and sensor rate audit flags, ensuring accurate forensic reporting when both services run concurrently on the same device (R-ID 367). (Resolved Sep.19.08)
*   **SOT ID 366**: Hardware Reset Integrity Hardening - Ensured `hardwareSuite.resetBaseline()` is invoked during session termination in both `TrackerService` and `ViewerService`. This prevents stale IMU peaks, adaptive vibration floors, and GNSS revival flags from persisting across monitoring sessions, maintaining high forensic integrity between restarts (R-ID 366). (Resolved Sep.19.07)
*   **SOT ID 365**: Proximity Suppression Hysteresis Hardening - Implemented temporal decay for the display flickering suppression logic in `HardwareSuite.kt`. By checking if the last display transition occurred within `DISPLAY_FLICKER_TIMEOUT_MS` (3s), the system now allows proximity "Far" transitions once flickering ceases, even without a further display event. This prevents suppression "lock-in" on stationary devices (R-ID 365). (Resolved Sep.19.06)
*   **SOT ID 364**: GNSS Initialization Race Remediation - Reordered state initialization in `HardwareSuite.start()` to ensure `sessionStartRt` and `lastFixRt` are populated before the `isStarted` flag is set. This prevents the background audit loop from calculating false GPS gaps against zeroed values during the micro-window of session activation (R-ID 364). (Resolved Sep.19.05)
*   **SOT ID 363**: Structured Concurrency Burst Hardening - Refactored GNSS revival pulse logic in `HardwareSuite.kt` to use a `try-finally` block within a single coroutine. This guarantees that Raw and Fused location listeners are unregistered upon burst completion or cancellation (e.g., during Safe Mode transition or suite shutdown), eliminating the risk of resource leaks and allowing for the removal of the redundant `revivalBurstJob` state (R-ID 363). (Resolved Sep.19.04)
*   **SOT ID 362**: GNSS Revival Resource Leak Remediation - Modified `setSafeMode(active)` in `HardwareSuite.kt` to explicitly unregister `rawRevivalListener` and `revivalCallback`. This ensures that raw GPS and high-accuracy updates are immediately terminated when Safe Mode is engaged, preventing resource leaks and unintended battery drain (R-ID 362). (Resolved Sep.19.02)
*   **SOT ID 361**: Redundant Battery Baseline Capture Remediation - Fully resolved the redundant battery baseline capture logic by initializing `lastFixRt` to `sessionStartRt` inside both `start()` and `resetBaseline()`. This guarantees a proper grace period before any GNSS gap or stall state can be declared, preventing immediate redundant baseline captures upon app activation or reset (R-ID 361). (Resolved Sep.19.03)
*   **SOT ID 360**: GNSS Stall Timing Leakage Remediation - Implemented explicit reset of revival state variables and guarded the background audit loop in `HardwareSuite.kt` with `isStarted.get()`. This prevents `pendingEnterRt` from accumulating stall duration while the suite is inactive, ensuring no immediate hardware locks occur upon activation (R-ID 360). (Resolved Sep.19.02)
*   **SOT ID 359**: Battery Baseline Persistence Hardening - Ensured that `revivalBaselineCaptured` flag in `HardwareSuite.kt` is reset to false within `stop()` and `resetBaseline()`. This prevents lifecycle persistence of the capture state, ensuring new battery baselines are correctly captured after suite restarts or manual resets (R-ID 359). (Resolved Sep.19.01)
*   **SOT ID 358**: Battery Baseline Capture Hardening - Implemented `revivalBaselineCaptured` flag in `HardwareSuite.kt` to ensure a single battery baseline capture per GNSS pending cycle. Prevents premature recapture when intermediate audit events consume the baseline during a sustained stall (R-ID 358). (Resolved Sep.19.00)
*   **SOT ID 357**: Structured Concurrency Burst Hardening - Resolved a coroutine leak in `HardwareSuite.kt` by tracking the 10-second raw GPS revival timeout via `revivalBurstJob`. Ensured immediate cancellation during suite teardown and safe mode transitions (R-ID 357). (Resolved Sep.18.00)

## 📈 Metric Summary
- **Rules Verified**: 76
- **Total SOT IDs**: 372
- **Resolved Issues**: 1118
- **Open Issues**: 2
- **Testing Coverage**: 2 (Sub-items: 10)
- **Simplification Ideas**: 19
- **QA Validation Tasks**: 282

## 🏁 Verification Chapters
*   **Chapter 31.36 (Activity-Denied Fallback)**: PASSED - Verified permission-gated WakeLock fallback in HardwareSuite (Sep.19.13)
*   **Chapter 31.35 (Async Registration)**: PASSED - Verified isStarted check in setPowerSaveMode task (Sep.19.13)
*   **Chapter 31.34 (Acoustic Lifecycle)**: PASSED - Verified acousticLock and definitive thread joining in startAcousticMonitoring (Sep.19.11)
*   **Chapter 31.33 (Buffer Integrity)**: PASSED - Verified circular buffer reset in stop() lifecycle (Sep.19.10)
*   **Chapter 31.32 (Snapshot Integrity)**: PASSED - Verified Volatile visibility and synchronized peak resets (Sep.19.09)
*   **Chapter 31.31 (Multi-Role Audit)**: PASSED - Verified role-based state separation in ForensicAuditor (Sep.19.08)
*   **Chapter 31.30 (Hardware Reset Integrity)**: PASSED - Verified hardwareSuite.resetBaseline() call in resetServiceTimers (Sep.19.07)
*   **Chapter 31.29 (Proximity Hysteresis)**: PASSED - Verified temporal decay logic for flickering suppression (Sep.19.06)
*   **Chapter 31.28 (Initialization Race)**: PASSED - Verified that lastFixRt is initialized before isStarted flag in start() (Sep.19.05)
*   **Chapter 31.27 (Structured Burst Cleanup)**: PASSED - Verified that Raw/Fused listeners are unregistered via try-finally on coroutine cancellation (Sep.19.04)
*   **Chapter 31.26 (Safe Mode Leakage)**: PASSED - Verified that revival listeners are unregistered when Safe Mode is toggled (Sep.19.02)
*   **Chapter 31.25 (Baseline Capture Gating)**: PASSED - Verified that battery baseline capture is strictly gated by suite active state (Sep.19.03)
*   **Chapter 31.24 (Stall Timing Leakage)**: PASSED - Verified that revival state resets cleanly on stop and audits are gated by suite lifecycle (Sep.19.02)

---
*Next Audit: Sep.20.00. (Sep.19.13)*
