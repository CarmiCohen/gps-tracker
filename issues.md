# Project Issues & Hardening Tracking (Aug.19.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 CRITICAL | 1 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 656 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #213-C1**: Status Logic Desync. "SIGNAL LOSS" is reported in the UI while GPS coordinates are still being updated on the map.
*   **Concern #212-C2 (Final Forensic Conclusion)**: Samsung CFMS Trigger is a **Resilient Static Heuristic**.
    *   **Evidence**: `libmbrainSDK` load attempts persist despite:
        1. JNI suppression (Aug.19.06).
        2. Metadata/Keyword rephrasing (Aug.19.06).
        3. Permission/Service-type stripping (Aug.19.06).
        4. **Identity Swap** (Package name changed to `com.gps19.forensic`) (Aug.19.08).
    *   **Implication**: The trigger is likely embedded in the APK resource signatures or internal class structure (e.g., `ViewRootImpl` hooks matching any app with a specific "Tracker" profile or resource manifest). Neutralization via standard manifest/identity changes is not possible.

---

## 🔴 Open Issues
*   **Issue #213: Signal Loss False-Positive**: Debug the `SystemStatusProvider` and `LocationProcessor` logic that triggers "UNCERTAINTY: SIGNAL LOSS" during active connectivity.

---

## 🟢 Recently Resolved Issues (Aug.19.08)
*   **Issue #212: Advanced Collision Forensic**: Exhausted all non-destructive methods to neutralize the Samsung CFMS `libmbrainSDK` trigger. Confirmed as a resilient OS-level heuristic. Restored functional state (vAug.19.08) and accepted the "Can't load libmbrainSDK" logcat noise as a benign vendor side-effect.
*   **Issue #214: System Issue Dashboard Audit**: Confirmed the "1" issue count and automatic setup navigation on Samsung A15 are intentional R405 safety mechanisms for battery exemption validation (R405).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.19.08)
