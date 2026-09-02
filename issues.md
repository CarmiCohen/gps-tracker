# Project Issues & Hardening Tracking (Sep.02.44)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 8 |
| **Validation Tasks** | 🟢 Validated | 222 |
| **Resolved (Total)** | 🟢 Progress | 829 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No high-priority risks identified in this session.*

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   **Issue #122**: Hardware Settling Window Verification (Confirming 800ms delay in `stop()` effectively prevents native race conditions).
*   **Issue #118**: 16KB Page Size Compatibility (Auditing secondary native dependencies for Android 15 alignment).
*   **Issue #120b**: SIT (Stationary State) field validation (Verifying high-precision forensic timestamps).

### Medium Priority (UX & Data Integrity)
*   **Issue #005**: Log Spillage Hardening (Final validation of silence on Samsung G990/A15 hardware).
*   **Issue #119**: Battery Steep Discharge Refinement (Hardening thresholds to prevent aggressive Power Save entries).
*   **Issue #180**: Proto-Mirror Parity Verification (Ensuring full consistency between `TrackerStatus` and `TrackerStatusProto`).

### Low Priority (Tech Debt & Auditing)
*   **Issue #197**: Forensic Teardown Timing Logs (Implementing high-precision logs for component unregistration).
*   **Idea #238**: Location Model Unification (Merging `LocationUpdate` and `LocationState` to reduce allocation churn).

---

## 🟢 Recently Resolved Issues (Sep.02.44)
*   **Issue #893 RESOLVED: Native Resource Disposal Leak Hardening**. Audited and verified Looper alignment for all `ManagedNetworkCallback` and `FusedLocationProvider` registrations, ensuring deterministic `BaseEventQueue` disposal on Android 15 (R893). (Sep.02.44).
*   **Issue #894 RESOLVED: ContextShadow Coverage Expansion**. Expanded `ContextShadow` delegate usage to silence `getPackageName` IPC diagnostic logs (R1.14). (Sep.02.43).
*   **Issue #898 RESOLVED: HUD Telemetry Stalled in Tracker Mode**. Integrated observation of `localLocation` and `trackerLocation` flows in `MainViewModel` (R3.1). (Sep.02.42).
*   **Issue #897 RESOLVED: Sensor Sensitivity Sliders Disconnected**. Propagated UI sensitivity settings (Vibration/Tilt) to the engine (R2.3). (Sep.02.41).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.44)*
*Simplification Ideas: 238 Active (2 Resolved)*
