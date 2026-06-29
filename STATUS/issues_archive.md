# Issues Archive (Historical Resolutions)

## 🗺️ Legacy Issue Mapping (Authoritative Unification)
The following legacy IDs from early development and hardening phases have been unified into the #300+ authoritative range.

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
*   **Issue #452**: Forensic SNR Latch Audit. Verified logic and test parity for 6-minute adaptive hold (R332). High-SNR jumps with zero vibration now correctly trigger the 360s sustained hold. (v8.9.55)
*   **Issue #458**: Watchdog Battery Optimization. Implemented conservative `AlarmManager` rescheduling to optimize hardware alarm cycles. (v8.9.55)

[Rest of archive content...]
