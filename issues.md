# Project Issues & Hardening Tracking (Sep.02.45)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 7 |
| **Validation Tasks** | 🟢 Validated | 223 |
| **Resolved (Total)** | 🟢 Progress | 830 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No high-priority risks identified in this session.*

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   **Issue #118**: 16KB Page Size Compatibility (Auditing secondary native dependencies for Android 15 alignment).
*   **Issue #120b**: SIT (Stationary State) field validation (Verifying high-precision forensic timestamps).

### Medium Priority (UX & Data Integrity)
*   **Issue #005**: Log Spillage Hardening (Final validation of silence on Samsung G990/A15 hardware).
*   **Issue #119**: Battery Steep Discharge Refinement (Hardening thresholds to prevent aggressive Power Save entries).
*   **Issue #180**: Proto-Mirror Parity Verification (Ensuring full consistency between `TrackerStatus` and `TrackerStatusProto`).

### Low Priority (Tech Debt & Auditing)
*   **Issue #197**: Forensic Teardown Timing Logs (Implementing high-precision logs for component unregistration - Verified in #122).
*   **Idea #238**: Location Model Unification (Merging `LocationUpdate` and `LocationState` to reduce allocation churn).

---

## 🟢 Recently Resolved Issues (Sep.02.45)
*   **Issue #122 RESOLVED: Hardware Settling Window Verification**. Enhanced `stop()` teardown with forensic duration tracking and summary reporting. Confirmed 800ms settling window effectively prevents native race conditions (R891). (Sep.02.45).
*   **Issue #893 RESOLVED: Native Resource Disposal Leak Hardening**. Audited and verified Looper alignment for all `ManagedNetworkCallback` and `FusedLocationProvider` registrations, ensuring deterministic `BaseEventQueue` disposal on Android 15 (R893). (Sep.02.44).
*   **Issue #894 RESOLVED: ContextShadow Coverage Expansion**. Expanded `ContextShadow` delegate usage to silence `getPackageName` IPC diagnostic logs (R1.14). (Sep.02.43).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.45)*
*Simplification Ideas: 239 Active (2 Resolved)*
