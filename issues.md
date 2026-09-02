# Project Issues & Hardening Tracking (Sep.02.46)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 5 |
| **Validation Tasks** | 🟢 Validated | 223 |
| **Resolved (Total)** | 🟢 Progress | 832 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No high-priority risks identified in this session.*

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   *No high-priority issues remaining.*

### Medium Priority (UX & Data Integrity)
*   **Issue #005**: Log Spillage Hardening (Final validation of silence on Samsung G990/A15 hardware).
*   **Issue #119**: Battery Steep Discharge Refinement (Hardening thresholds to prevent aggressive Power Save entries).
*   **Issue #180**: Proto-Mirror Parity Verification (Ensuring full consistency between `TrackerStatus` and `TrackerStatusProto`).

### Low Priority (Tech Debt & Auditing)
*   **Issue #197**: Forensic Teardown Timing Logs (Implementing high-precision logs for component unregistration - Verified in #122).
*   **Idea #238**: Location Model Unification (Merging `LocationUpdate` and `LocationState` to reduce allocation churn).

---

## 🟢 Recently Resolved Issues (Sep.02.46)
*   **Issue #118 RESOLVED: 16KB Page Size Compatibility**. Confirmed native library `jdHardware` alignment using `-Wl,-z,max-page-size=16384` and set `extractNativeLibs="false"` for Android 15 compliance (R895). (Sep.02.46).
*   **Issue #120b RESOLVED: SIT Forensic Timestamp Validation**. Verified end-to-end propagation of high-precision timestamps (`sitVzTs`, `sitVzRt`) from detection in `LocationSentinel` through the aggregator to Room persistence (R172). (Sep.02.46).
*   **Issue #122 RESOLVED: Hardware Settling Window Verification**. Enhanced `stop()` teardown with forensic duration tracking (R891). (Sep.02.45).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.46)*
*Simplification Ideas: 239 Active (2 Resolved)*
