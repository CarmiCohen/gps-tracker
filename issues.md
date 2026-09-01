# Project Issues & Hardening Tracking (Sep.01.14)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Needs Action | 21 |
| **Validation Tasks** | 🟢 Validated | 218 |
| **Resolved (Total)** | 🟢 Progress | 807 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None identified in this session.

---

## 🔴 Open Issues
*   *(See Dashboard for total count)*

---

## 🟢 Recently Resolved Issues (Sep.01.14)
*   **Issue #887 RESOLVED: Native BaseEventQueue leak remediation (R887)**. Hardened `ManagedHardware.kt` by increasing unregistration timeouts to 4000ms and implementing a final direct fallback unregistration on the current thread if the asynchronous attempt times out. This prevents native `BaseEventQueue` leaks during high-load stalls on SM-A155F. (Sep.01.14).
*   **Issue #886 RESOLVED: Monitor::Inflate timing race (R886)**. Confirmed that sequential init (R884) ensures library residence, but a settling delay was required for framework synchronization. Added a 500ms post-init window in `TrackerService` and `ViewerService` before GNSS stack registration. (Sep.01.13).
*   **Issue #885 RESOLVED: Level 8 Hydration Davey Remediation (R885)**. Decomposed the monolithic overlay hydration into 4 individual levels (8-11). Each heavy component (Settings, Log, Ribbons, GNSS) now has a dedicated hydration delay to distribute JIT compilation load on SM-A155F hardware. (Sep.01.13).
*   **Issue #884 RESOLVED: Monitor::Inflate initialization failure regression (R884)**. Hardened `JdHardwareManager` initialization by switching to sequential invocation in `TrackerService` and `ViewerService` on A15 devices. (Sep.01.11).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.01.14)*
*Simplification Ideas: 227*
