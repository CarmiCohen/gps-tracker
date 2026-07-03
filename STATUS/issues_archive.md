# Issues Archive (Historical Resolutions)

## Hardening Phase: v8.9.78 Resolved Items
*   **Issue #018**: Tracker Behavior Stability (Stationary Anchor Hard-Lock). Implemented coordinate clamping to `parkingAnchorPoint` when `stationaryProb > 0.9`. Added breakout logic for spatial displacements > 20m. (v8.9.78)
*   **Issue #019**: Android 14+ "While-in-Use" Permission Transition. Implemented `isRecentUiPulse()` window (15s) to authorize background-to-foreground service transitions for sensitive types. (v8.9.78)
*   **Issue #014**: System-Wide Type Safety. Standardized all telemetry fields (Accuracy, Speed, Bearing) to native `Double` types across the entire stack, eliminating conversion overhead. (v8.9.75)
*   **Issue #015**: StandaloneCoroutine Cancellation. Hardened `SyncManager` and `CommandRouter` to silently handle `CancellationException` during service lifecycle transitions. (v8.9.72)
*   **Issue #011**: Suppression Forensic Labeling. Implemented `suppressionNote` in `SentinelResult` to provide transparency when hardware muzzles suppress sensor violations. (v8.9.68)
*   **Issue #010**: A15 Acoustic/Vibration Coherence. Implemented physical reality gate; acoustic spikes on A15 are suppressed if concurrent vibration is below threshold. (v8.9.68)
*   **Issue #013**: Forensic UI Expansion. Exposed internal scaling metrics (`proximityDebounceMs`, `vibrationRollingSum`) to the UI dashboard. (v8.9.71)

## Hardening Phase: v8.9.65 Resolved Items
*   **Issue #R325**: Samsung A15 Accuracy Truncation. Optimized status row layout width (210dp) to ensure authoritative uncertainty display fits narrow screens. (v8.9.65)
*   **Issue #006**: Samsung A15 Main Thread Jitter. Offloaded high-frequency sensor event processing to a dedicated `HandlerThread` (`AppSensorThread`). (v8.9.64)
*   **Issue #007**: Connectivity Rejoin Latency. Implemented reactive `ConnectionLostCallback` for immediate signaling re-join. (v8.9.64)
*   **Issue #008**: VID_NOTES Correction. Updated note identifier to "Th1030" for role alignment. (v8.9.73)
*   **Issue #461**: Settings Uniqueness UI Feedback. Implemented error propagation from `SettingsRepository` to UI via Toast. (v8.9.63)
*   **Issue #001**: Room Schema Divergence. Incremented DB to v51 and corrected historical migrations. (v8.9.62)
*   **Issue #002**: GPS Status UI Mismatch. Increased failure thresholds to 35s. (v8.9.62)
*   **Issue #003**: Main Thread Jitter (Davey). Moved behavioral state computations to `Dispatchers.Default` in `MainViewModel`. (v8.9.62)
*   **Issue #004**: A15 Virtual Proximity Suppression. Refined manager to allow 'Far' transitions during motion in darkness. (v8.9.62)
*   **Issue #005**: Map Provider Log Spillage. Silenced `osmdroid` debug logs. (v8.9.62)

---

## 🗺️ Legacy Issue Mapping (Authoritative Unification)
The following legacy IDs have been unified into the #300+ authoritative range.

| Legacy ID | Authoritative ID | Category / Description |
| :--- | :--- | :--- |
| #115 | #322 | Architectural Bloat: ViewModel Decoupling |
| #148 | #453 | Samsung A15 GPS Stalling |
| #180 | #340 | Samsung A15 Proximity Limitation |
| #190 | #455 | Xiaomi Autostart & Boot Resilience |
| #191 | #454 | Samsung A15 Proximity Flutter |
| #214-A | #325-B | Unified Accuracy Fallback Logic |
| #214-M | #347 | Stale Legacy Reference Migration |
| #219 | #332 | SNR-IMU Correlation Validation |
| #220 | #334 | Hindsight Trajectory Correction |
| #221 | #328-B | Bayesian Uncertainty / systemPulseRealtime |
| #224 | #329 | Forensic Ribbon Expansion (tiltIdx/baroIdx) |
| #227 | #327 | Hindsight Transition Smoothing |
| #244/245 | #339/348 | SIT Rising-Edge Detection |
| #263 | #369 | EMA Constant Inversion Audit |
| #264 | #345 | GtoEngine Magic Number Consolidation |
| #265 | #370 | TrackerService Redundant Evaluation Audit |
| #266 | #388 | Lux EMA Implementation |
| #267 | #463-B | Dead Code Cleanup: isRevivalTriggered |
| #268 | #352 | Acoustic Floor Logic Redundancy |
| #271 | #357 | Uptime Consistency (uptimeMs) |
| #272 | #353 | Battery Profile (app_settings.proto) |
| #273 | #315 | Network Integrity & Timeout Scaling |
| #279 | #351 | Foreground Resilience Hardening |
| #281 | #462 | SoT Naming Alignment |
| #284 | #389 | Light EMA Logic Inconsistency |
| #285 | #367 | GtoEngine Implementation |
| #286 | #368 | Hardcoded EMA Cleanup (LUX_EMA_FAST) |
| #287 | #331 | Role-Aware Alert Title Visibility |
| #288 | #349 | Vertical Displacement Failure |
| #289 | #344 | Dead State Cleanup (Revival Flag) |
| #291 | #358 | SIT Forensic Duplicate Risk |
| #292 | #343 | Acoustic Floor Decay Logic |
| #293 | #464 | Geofence Evaluation Bug (Viewer) |
| #294 | #387-B | Viewer Offline Detection Logic Gap |
| #295 | #390-B | Redundant Barometric Baselining |
| #296 | #335 | serviceStartRealtime Initialization Gap |
| #297 | #359 | Hindsight Promotion Coverage |
| #302 | #385 | Behavioral Magic Numbers |
| #303 | #386-B | Trajectory Gating Multiplier |
| #306 | #321 | Shadow Constants Remediation |
| #336-E | #459 | Chair Sit Detection (R832) |
| #336-G | #336-B | SIT Duplicate Guard |
| #354 | #463 | Battery Alarm Threshold |
| #354-B | #463-B | Dead Code Cleanup |
| #355-B | #462 | SoT Naming Alignment |
| #360-J | #387 | Logic Alignment - Jump Threshold |
| #360-V | #387-B | Viewer Offline Detection |
| #361-B | #390-B | Barometric Baselining |
| #361-D | #390 | Documentation Refactor |
| #362-K | #386 | Xiaomi Key Naming |
| #362-T | #386-B | Trajectory Gating Multiplier |
| #363-H | #453 | Samsung GPS Hardware |
| #364-H | #454 | Samsung Proximity Hardware |
| #364-L | #426 | GPS Staleness Logic |
| #365-H | #455 | Xiaomi Autostart Hardware |
| #365-L | #457 | Ghost Mode Status Conflict |
| #366-R | #456 | Resilience Hardening - Watchdog |
| #366-W | #458 | Watchdog Battery Optimization |

---

## Hardening Phase: v8.9.55 Resolved Items
*   **Issue #452**: Forensic SNR Latch Audit. Verified 6-minute adaptive hold (R332). (v8.9.55)
*   **Issue #458**: Watchdog Battery Optimization. Implemented conservative `AlarmManager` rescheduling. (v8.9.55)
