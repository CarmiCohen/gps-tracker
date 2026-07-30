# Project Issues & Hardening Tracking (July.30.23)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 4 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 462 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   **[Issue #626] [Severity: High] [Category: Stability] Foreground Service Start Restriction**.
    - **Observed**: `ForegroundServiceStartNotAllowedException` during automatic restoration (R405 flow) when the screen is off.
    - **Status**: 🟡 Partially Resolved. Stabilized in `MainActivity.kt` via try-catch to prevent fatal crash loops.
    - **Requirement**: Refactor restoration logic to comply with Android 12+ foreground policies (e.g., delay start until `onResume` or use `WorkManager`).

*   **[Issue #627] [Severity: High] [Category: Performance] Startup ANR & Main Thread Blocking**.
    - **Observed**: Recurring 4.3s+ UI stall and "App Not Responding" dialog during cold start on Samsung A15.
    - **Concern**: Main thread is heavily congested during `TrackerService` initialization and `libmbrainSDK` loading. 
    - **Requirement**: Offload native library loading and hardware initialization to background coroutines.

*   **[Issue #628] [Severity: Med] [Category: Compatibility] 16KB Page Size Support**.
    - **Observed**: OS-level compatibility warning flagging non-aligned libraries: `libdatastore_shared_counter.so`, `libmbrainSDK.so`, `libandroidx.graphics.path.so`.
    - **Requirement**: Native libraries must be re-aligned/re-compiled for 16KB page size compatibility.

*   **[Issue #625] [Severity: Med] [Category: Structural] Structural: Mbrain JNI Reliability Audit**.
    - **Context**: The `MbrainHardwareManager` utilizes native JNI calls for hardware optimization. Native calls can be interrupted by system signals (EINTR).
    - **Requirement**: Harden the Kotlin bridge to handle native call results robustly.

---

## 🟢 Recently Resolved Issues (July.30.23)
*   **[Issue #624] [Severity: Med] [Category: Forensic] Forensic: System Integrity Periodic Check**.
    - **Resolution**: Implemented background heartbeat mechanism in `IntegrityMonitor.kt`.
    - **Impact**: Ensures monitoring vital signs and prevents silent OS-level failures.
    - **Validation**: Verified requirement alignment (**R624**).

*   **[Issue #623] [Severity: Low] [Category: Structural] Structural: Latency Monitor Metric Cleanup**.
    - **Resolution**: Completed cleanup of forensic spike reporting and implemented `measureAndAudit` API.
    - **Impact**: Centralizes forensic naming and eliminates string duplication.
    - **Validation**: Verified requirement alignment (**R623**).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
