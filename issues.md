# Project Issues & Hardening Tracking (Sep.02.50)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 2 |
| **Validation Tasks** | 🟢 Validated | 224 |
| **Resolved (Total)** | 🟢 Progress | 837 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No high-priority risks identified in this session.*

---

## 🔴 Open Issues (Prioritized)

### High Priority (Stability & Compliance)
*   *No high-priority issues remaining.*

### Medium Priority (UX & Data Integrity)
*   **Issue #119**: Battery Steep Discharge Refinement (Hardening thresholds to prevent aggressive Power Save entries).
*   **Issue #180**: Proto-Mirror Parity Verification (Ensuring full consistency between `TrackerStatus` and `TrackerStatusProto`).

### Low Priority (Tech Debt & Auditing)
*   *All low-priority items resolved.*

---

## 🟢 Recently Resolved Issues (Sep.02.50)
*   **Issue #005 RESOLVED: Log Spillage Hardening**. Replaced all direct `android.util.Log` calls with `Timber` across the app module. Ensured that diagnostic logs are strictly silenced in release builds to prevent spillage on Samsung G990/A15 hardware (R759). (Sep.02.50).
*   **Issue #197 RESOLVED: Forensic Teardown Timing Logs**. Implemented high-precision duration tracking in `ConnectivitySuite` and `CommunicationManager` to ensure teardown auditing parity with the hardware layer (Sep.03.01).
*   **Issue #238 RESOLVED: Location Model Unification**. Merged `LocationUpdate` and `LocationState` models, promoting the core engine model as the unified structure to eliminate allocation churn and mapping overhead (Sep.03.01).
*   **Issue #118 RESOLVED: 16KB Page Size Compatibility**. Confirmed native library `jdHardware` alignment using `-Wl,-z,max-page-size=16384` and set `extractNativeLibs="false"` for Android 15 compliance (R895). (Sep.02.46).
*   **Issue #120b RESOLVED: SIT Forensic Timestamp Validation**. Verified end-to-end propagation of high-precision timestamps (`sitVzTs`, `sitVzRt`) from detection in `LocationSentinel` through the aggregator to Room persistence (R172). (Sep.02.46).
*   **Issue #122 RESOLVED: Hardware Settling Window Verification**. Enhanced `stop()` teardown with forensic duration tracking (R891). (Sep.02.45).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.02.50)*
*Simplification Ideas: 238 Active (4 Resolved)*
