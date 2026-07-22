# Project Issues & Hardening Tracking (July.22.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 317 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #122: SIT Propagation Depth**: Forensic fields are now 15+ deep (snrIdx, noiseIdx, luxIdx, vibeIdx, liftIdx, etc.). Relay-server must be verified for JSON/Proto compatibility with this expanded payload.
*   **Issue #121: Provider Latency**: Circularity resolution via `Provider<T>` is stable but introduces minor lookup overhead in `LogManager`.
*   **Issue #120b: Budget Hardware Initialization Spikes**: Budget devices (A15) remain sensitive. The 500ms staggered startup is critical.
*   **Issue #119: Boot Persistence Integrity**: `isSystemActive` is the single source of truth for service revival.

---

## 🔴 Open Issues
### Issue #113: R405c Fallback Efficacy Verification (Samsung A15)
*   **Description**: Perform long-term field testing on SM-A155F to confirm Accelerometer-based pulse prevents OS-level eviction.

---

## 🟢 Recently Resolved Issues (July.22.01)
*   **Issue #118: Final Forensic Parity Synchronization**.
    *   **Resolution**: Standardized 15+ forensic parameters (`snrIdx`, `noiseIdx`, `luxIdx`, `vibeIdx`, `liftIdx`, `proxIdx`, and SIT fields) across `LocationUpdate`, `TrackerStatus`, `SystemHealthState`, `HistoryEntity`, and Protobuf (`RealtimeStatus`).
*   **Issue #120: Hilt Hardening & Dependency Restoration**.
    *   **Resolution**: Systematically added `@Inject` constructors and `@Singleton` annotations to 20+ core components. Resolved circularities using `Provider<T>`.
*   **Issue #123: Version Consolidation**.
    *   **Resolution**: Updated all version references to `July.22.01` and synchronized `app/build.gradle`.

## 🟢 Recently Resolved Issues (July.20.07)
*   **Issue #117: ViewerService Compilation Restoration**.
*   **Issue #107: Step Detector Hardware Registration Hardening**.
*   **Issue #114: Monotonic Timeline Boundary Audit**.
*   **Issue #115: Startup Scope Hardening (GlobalScope Removal)**.
